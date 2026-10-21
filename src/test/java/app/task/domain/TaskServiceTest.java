package app.task.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import app.exceptions.BadRequestException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.EnumSource;

import app.competence.domain.Competence;
import app.employee.domain.Employee;
import app.exceptions.NotFoundException;
import app.persistence.testdoubles.EntityIds;
import app.persistence.testdoubles.InMemoryCompetenceDAO;
import app.persistence.testdoubles.InMemoryEmployeeDAO;
import app.persistence.testdoubles.InMemoryStageDAO;
import app.project.domain.Project;
import app.project.domain.ProjectStatus;
import app.stage.domain.Stage;
import app.task.data.ITaskDAO;
import app.task.presentation.dto.TaskCreateDTO;
import app.employee.presentation.dto.EmployeeReferenceDTO;
import app.task.presentation.dto.TaskDTO;
import app.task.presentation.dto.TaskEmployeesDTO;
import app.task.presentation.dto.TaskUpdateDTO;

class TaskServiceTest
{
    private static final double TOLERANCE = 1e-9;

    private InMemoryTaskDAO taskDAO;
    private InMemoryStageDAO stages;
    private InMemoryEmployeeDAO employees;
    private TaskService taskService;

    /**
     * Seeds tasks 1, 2 and 3 in project 1 and task 4 in project 2. Task 2 depends on task 1, and task 3 on task 2.
     * Competence 1 (carpenter) and 3 (painter) are active, competence 2 is inactive. Task 1 requires carpenter;
     * tasks 2 and 3 have no competence. Employees: 1 and 2 are active carpenters, 3 is active without competences,
     * 4 is an active carpenter (deactivated by tests that need it), and 5 is an active carpenter and painter.
     */
    @BeforeEach
    void setUp()
    {
        InMemoryStageDAO stageDAO = new InMemoryStageDAO();
        stages = stageDAO;
        Stage planning = stageDAO.create(new Stage("Planning", project(1L)));
        Stage otherProjectStage = stageDAO.create(new Stage("Other project", project(2L)));

        InMemoryCompetenceDAO competenceDAO = new InMemoryCompetenceDAO();
        Competence carpenter = competenceDAO.create(new Competence("carpenter", new BigDecimal("850.00")));
        competenceDAO.create(new Competence("plumber", new BigDecimal("800.00"))).setActive(false);
        Competence painter = competenceDAO.create(new Competence("painter", new BigDecimal("700.00")));

        taskDAO = new InMemoryTaskDAO();
        Task requirements = taskDAO.create(new Task(planning, "Requirements", 1));
        requirements.setCompetence(carpenter, 8.0);
        Task design = taskDAO.create(new Task(planning, "Design", 1));
        Task build = taskDAO.create(new Task(planning, "Build", 1));
        taskDAO.create(new Task(otherProjectStage, "Other project task", 1));
        design.addPredecessor(requirements);
        build.addPredecessor(design);

        InMemoryEmployeeDAO employeeDAO = new InMemoryEmployeeDAO();
        employees = employeeDAO;
        employeeDAO.create(new Employee("Alice", "Worker", 7.5)).addCompetence(carpenter);
        employeeDAO.create(new Employee("Bob", "Builder", 6.0)).addCompetence(carpenter);
        employeeDAO.create(new Employee("Carl", "Helper", 7.5));
        employeeDAO.create(new Employee("Dina", "Former", 7.5)).addCompetence(carpenter);
        Employee eva = employeeDAO.create(new Employee("Eva", "Allround", 7.5));
        eva.addCompetence(carpenter);
        eva.addCompetence(painter);

        taskService = new TaskService(taskDAO, stageDAO, competenceDAO, employeeDAO);
    }


    @ParameterizedTest
    @EnumSource(value = Task.TaskStatus.class, names = {"NOT_STARTED", "IN_PROGRESS"})
    @DisplayName("Completion records the updated estimate and minimum duration from either unfinished status")
    void completionRecordsUpdatedPlan(Task.TaskStatus initialStatus)
    {
        taskService.update(1L, new TaskUpdateDTO(null, null, null, null, initialStatus));
        taskService.assignEmployee(1L, 1L);

        taskService.update(1L, new TaskUpdateDTO(null, 5, 1L, 30.0, Task.TaskStatus.DONE));

        Task stored = taskDAO.getStored(1L);
        assertThat(stored.getStatus(), is(Task.TaskStatus.DONE));
        assertThat(stored.hasPlanningSnapshot(), is(true));
        assertThat(stored.getCompletedStartDate(), is(LocalDate.of(2030, 1, 7)));
        assertThat(stored.getCompletedEndDate(), is(LocalDate.of(2030, 1, 11)));
        assertThat(stored.getCompletedLaborDurationInDays(), is(4.0));
        assertThat(stored.getCompletedScheduledDurationInDays(), is(5.0));
    }

    @Test
    @DisplayName("Repeated DONE and ordinary edits preserve the original snapshot")
    void completedTaskUpdatesDoNotOverwriteSnapshot()
    {
        TaskUpdateDTO done = new TaskUpdateDTO(null, null, null, null, Task.TaskStatus.DONE);
        taskService.update(1L, done);
        Task stored = taskDAO.getStored(1L);
        var recordedAt = stored.getPlanningSnapshotRecordedAt();
        LocalDate originalEnd = stored.getCompletedEndDate();
        Double originalLabor = stored.getCompletedLaborDurationInDays();
        Double originalScheduled = stored.getCompletedScheduledDurationInDays();

        taskService.update(1L, new TaskUpdateDTO("Renamed", 10, 1L, 75.0, null));
        taskService.update(1L, done);

        assertThat(stored.getPlanningSnapshotRecordedAt(), is(recordedAt));
        assertThat(stored.getCompletedEndDate(), is(originalEnd));
        assertThat(stored.getCompletedLaborDurationInDays(), is(originalLabor));
        assertThat(stored.getCompletedScheduledDurationInDays(), is(originalScheduled));
    }

    @ParameterizedTest
    @EnumSource(value = Task.TaskStatus.class, names = {"NOT_STARTED", "IN_PROGRESS"})
    @DisplayName("Completing a reopened task replaces its snapshot with the new plan")
    void recompletingReopenedTaskRecordsNewPlan(Task.TaskStatus reopenedStatus)
    {
        TaskUpdateDTO done = new TaskUpdateDTO(null, null, null, null, Task.TaskStatus.DONE);
        taskService.update(1L, done);

        taskService.update(1L, new TaskUpdateDTO(null, 5, 1L, 30.0, reopenedStatus));
        assertThat(taskDAO.getStored(1L).getStatus(), is(reopenedStatus));
        taskService.update(1L, done);

        Task stored = taskDAO.getStored(1L);
        assertThat(stored.getCompletedEndDate(), is(LocalDate.of(2030, 1, 11)));
        assertThat(stored.getCompletedLaborDurationInDays(), is(4.0));
        assertThat(stored.getCompletedScheduledDurationInDays(), is(5.0));
    }

    @Test
    @DisplayName("Completion captures unavailable capacity before DONE includes inactive employees")
    void completionRecordsUndeterminedLivePlan()
    {
        taskService.assignEmployee(1L, 1L);
        employees.setActive(1L, false);

        taskService.update(1L, new TaskUpdateDTO(null, null, null, null, Task.TaskStatus.DONE));

        Task stored = taskDAO.getStored(1L);
        assertThat(stored.hasPlanningSnapshot(), is(true));
        assertThat(stored.getCompletedStartDate(), nullValue());
        assertThat(stored.getCompletedEndDate(), nullValue());
        assertThat(stored.getCompletedLaborDurationInDays(), nullValue());
        assertThat(stored.getCompletedScheduledDurationInDays(), nullValue());
    }

    @Test
    @DisplayName("Invalid completion does not change status or create a snapshot")
    void invalidCompletionDoesNotRecordSnapshot()
    {
        assertThrows(BadRequestException.class,
                () -> taskService.update(1L, new TaskUpdateDTO(null, null, null, 30.0, Task.TaskStatus.DONE)));

        assertThat(taskDAO.getStored(1L).getStatus(), is(Task.TaskStatus.NOT_STARTED));
        assertThat(taskDAO.getStored(1L).hasPlanningSnapshot(), is(false));
    }

    @Test
    @DisplayName("Cross-stage task dependencies are invalid even inside the same project")
    void crossStageTaskDependencyIsRejected()
    {
        Stage otherStage = stages.create(new Stage("Delivery", project(1L)));
        // This test deliberately uses the same project id but a different stage id.
        long otherTaskId = taskService.create(new TaskCreateDTO(otherStage.getId(), "Delivery task", 1L, 0.0, 1)).id();
        assertThrows(BadRequestException.class, () -> taskService.addPredecessor(otherTaskId, 1L));
    }

    @Test
    @DisplayName("Blocked task cannot skip directly to DONE or enter IN_PROGRESS")
    void blockedTaskCannotProgress()
    {
        TaskUpdateDTO progress = new TaskUpdateDTO(null, null, null, null, Task.TaskStatus.IN_PROGRESS);
        TaskUpdateDTO done = new TaskUpdateDTO(null, null, null, null, Task.TaskStatus.DONE);
        assertThrows(BadRequestException.class, () -> taskService.update(2L, progress));
        assertThrows(BadRequestException.class, () -> taskService.update(2L, done));
        taskService.update(1L, done);
        taskService.update(2L, progress);
        assertEquals(Task.TaskStatus.IN_PROGRESS, taskService.get(2L).status());
    }

    @Test
    @DisplayName("Cannot reopen a predecessor once its successor has started")
    void cannotReopenFinishedPredecessorAfterSuccessorStarts()
    {
        TaskUpdateDTO done = new TaskUpdateDTO(null, null, null, null, Task.TaskStatus.DONE);
        TaskUpdateDTO progress = new TaskUpdateDTO(null, null, null, null, Task.TaskStatus.IN_PROGRESS);
        TaskUpdateDTO reopen = new TaskUpdateDTO(null, null, null, null, Task.TaskStatus.NOT_STARTED);
        taskService.update(1L, done);
        taskService.update(2L, progress);
        assertThrows(BadRequestException.class, () -> taskService.update(1L, reopen));
    }

    @ParameterizedTest(name = "[{index}] {6}")
    @CsvFileSource(resources = "/testcases/task/task-service/create-fields.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Create - validates the stage, name, competence, estimate and minimum duration")
    void createValidatesFields(Long stageId, String name, Long competenceId, Double estimate,
                               Integer minimumDurationInDays, String expected, String reason)
    {
        TaskCreateDTO dto = new TaskCreateDTO(stageId, name, competenceId, estimate, minimumDurationInDays);

        if (expected.equals("ACCEPT"))
        {
            TaskDTO result = taskService.create(dto);
            assertThat(result.name(), is(name.trim()));
            assertThat(result.status(), is(Task.TaskStatus.NOT_STARTED));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> taskService.create(dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {7}")
    @CsvFileSource(resources = "/testcases/task/task-service/update-fields.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Update - validates supplied fields and the competence and estimate combination")
    void updateValidatesFields(Long id, String name, Integer minimumDurationInDays, Long competenceId, Double estimate,
                               Task.TaskStatus status, String expected, String reason)
    {
        TaskUpdateDTO dto = new TaskUpdateDTO(name, minimumDurationInDays, competenceId, estimate, status);

        if (expected.equals("ACCEPT"))
        {
            TaskDTO before = taskService.get(id);
            taskService.update(id, dto);
            TaskDTO stored = taskService.get(id);

            assertThat(stored.name(), is(name == null ? before.name() : name.trim()));
            assertThat(stored.minimumDurationInDays(),
                    is(minimumDurationInDays == null ? before.minimumDurationInDays() : minimumDurationInDays));
            assertThat(stored.status(), is(status == null ? before.status() : status));
            if (competenceId == null)
            {
                assertThat(stored.competence(), is(before.competence()));
                assertThat(stored.estimate(), is(before.estimate()));
            }
            else if (competenceId == 0)
            {
                assertThat(stored.competence(), nullValue());
                assertThat(stored.estimate(), is(0.0));
            }
            else
            {
                assertThat(stored.competence().id(), is(competenceId));
                assertThat(stored.estimate(), is(estimate));
            }
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> taskService.update(id, dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/task/task-service/add-predecessor.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Add predecessor - accepts only a new, acyclic dependency within one project")
    void addPredecessorValidatesDependency(Long taskId, Long predecessorId, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            taskService.addPredecessor(taskId, predecessorId);
            assertThat(taskService.get(taskId).predecessorIds(), hasItem(predecessorId));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> taskService.addPredecessor(taskId, predecessorId));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(resources = "/testcases/task/task-service/replace-employees.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Replace employees - validates the task and every employee id before storing the selection")
    void replaceEmployeesValidatesRequest(Long taskId, String employeeIds, int expectedCount, String expected, String reason)
    {
        TaskEmployeesDTO dto = new TaskEmployeesDTO(parseIds(employeeIds));

        if (expected.equals("ACCEPT"))
        {
            TaskDTO result = taskService.replaceEmployees(taskId, dto);
            assertThat(result.assignedEmployees().size(), is(expectedCount));
            assertThat(taskService.get(taskId).assignedEmployees().size(), is(expectedCount));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> taskService.replaceEmployees(taskId, dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/task/task-service/assign-employee.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Assign employee - accepts only an unassigned, existing employee on an existing task")
    void assignEmployeeValidatesRequest(Long taskId, Long employeeId, String expected, String reason)
    {
        taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(2L)));

        if (expected.equals("ACCEPT"))
        {
            taskService.assignEmployee(taskId, employeeId);
            assertThat(assignedEmployeeIds(taskId), is(List.of(1L, 2L)));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> taskService.assignEmployee(taskId, employeeId));
            assertThat(exception.getClass().getSimpleName(), is(expected));
            assertThat(assignedEmployeeIds(1L), is(List.of(2L)));
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/task/task-service/unassign-employee.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Unassign employee - requires an existing assignment")
    void unassignEmployeeValidatesRequest(Long taskId, Long employeeId, String expected, String reason)
    {
        taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(2L)));

        if (expected.equals("ACCEPT"))
        {
            taskService.unassignEmployee(taskId, employeeId);
            assertThat(assignedEmployeeIds(taskId), is(List.of()));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> taskService.unassignEmployee(taskId, employeeId));
            assertThat(exception.getClass().getSimpleName(), is(expected));
            assertThat(assignedEmployeeIds(1L), is(List.of(2L)));
        }
    }

    @Test
    @DisplayName("Unassign employee - keeps the other assignments")
    void unassigningOneEmployeeKeepsTheOthers()
    {
        taskService.assignEmployee(1L, 1L);
        taskService.assignEmployee(1L, 2L);

        taskService.unassignEmployee(1L, 1L);

        assertThat(assignedEmployeeIds(1L), is(List.of(2L)));
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(resources = "/testcases/task/task-service/assignment-eligibility.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Assignment eligibility - new employees must be active and hold the task's competence")
    void newAssignmentsMustBeEligible(String path, Long taskId, Long employeeId, String expected, String reason)
    {
        employees.setActive(4L, false);
        Runnable assignment = path.equals("REPLACE")
                ? () -> taskService.replaceEmployees(taskId, new TaskEmployeesDTO(List.of(employeeId)))
                : () -> taskService.assignEmployee(taskId, employeeId);

        if (expected.equals("ACCEPT"))
        {
            assignment.run();
            assertThat(assignedEmployeeIds(taskId), hasItem(employeeId));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, assignment::run);
            assertThat(exception.getClass().getSimpleName(), is(expected));
            assertThat(assignedEmployeeIds(taskId), is(List.of()));
        }
    }

    @Test
    @DisplayName("Assignment eligibility - an assigned employee who is later deactivated can be retained and removed")
    void retainedInactiveAssignmentIsNotANewAssignment()
    {
        taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(4L)));
        employees.setActive(4L, false);

        taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(4L, 1L)));
        assertThat(assignedEmployeeIds(1L), is(List.of(1L, 4L)));

        taskService.unassignEmployee(1L, 4L);
        assertThat(assignedEmployeeIds(1L), is(List.of(1L)));
    }

    @Test
    @DisplayName("Assignment eligibility - a deactivated employee cannot be re-assigned once removed")
    void removedInactiveEmployeeCannotBeAssignedAgain()
    {
        taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(4L)));
        employees.setActive(4L, false);
        taskService.unassignEmployee(1L, 4L);

        assertThrows(BadRequestException.class, () -> taskService.assignEmployee(1L, 4L));
        assertThrows(BadRequestException.class,
                () -> taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(4L))));
    }

    @Test
    @DisplayName("Assignment eligibility - an employee with several matching competences is stored once")
    void employeeWithSeveralCompetencesIsStoredOnce()
    {
        TaskDTO result = taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(5L, 5L)));

        assertThat(result.assignedEmployees().size(), is(1));
    }

    @Test
    @DisplayName("Assignment eligibility - one ineligible employee rejects the whole replacement")
    void ineligibleEmployeeRejectsWholeReplacement()
    {
        taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(1L)));

        assertThrows(BadRequestException.class,
                () -> taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(1L, 2L, 3L))));

        assertThat(assignedEmployeeIds(1L), is(List.of(1L)));
    }

    @Test
    @DisplayName("Assignment eligibility - assignments on a completed task cannot be changed")
    void completedTaskAssignmentsAreLocked()
    {
        taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(1L)));
        taskService.update(1L, new TaskUpdateDTO(null, null, null, null, Task.TaskStatus.DONE));

        assertThrows(BadRequestException.class,
                () -> taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(1L, 2L))));
        assertThrows(BadRequestException.class, () -> taskService.assignEmployee(1L, 2L));
        assertThrows(BadRequestException.class, () -> taskService.unassignEmployee(1L, 1L));

        assertThat(assignedEmployeeIds(1L), is(List.of(1L)));
    }

    @Test
    @DisplayName("Competence change - rejected while an assigned employee lacks the new competence")
    void competenceChangeIsRejectedWhileAssignedEmployeesLackIt()
    {
        taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(1L)));

        assertThrows(BadRequestException.class,
                () -> taskService.update(1L, new TaskUpdateDTO(null, null, 3L, 8.0, null)));

        TaskDTO task = taskService.get(1L);
        assertThat(task.competence().id(), is(1L));
        assertThat(assignedEmployeeIds(1L), is(List.of(1L)));
    }

    @Test
    @DisplayName("Competence change - accepted when every assigned employee holds the new competence")
    void competenceChangeIsAcceptedWhenAssignedEmployeesHoldIt()
    {
        taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(5L)));

        TaskDTO task = taskService.update(1L, new TaskUpdateDTO(null, null, 3L, 8.0, null));

        assertThat(task.competence().id(), is(3L));
        assertThat(assignedEmployeeIds(1L), is(List.of(5L)));
    }

    @Test
    @DisplayName("Competence change - removing the competence keeps the assignments")
    void removingCompetenceKeepsAssignments()
    {
        taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(1L)));

        TaskDTO task = taskService.update(1L, new TaskUpdateDTO(null, null, 0L, null, null));

        assertThat(task.competence(), is(nullValue()));
        assertThat(assignedEmployeeIds(1L), is(List.of(1L)));
    }

    @Test
    @DisplayName("Replace employees - rejects a missing request body")
    void replaceEmployeesRejectsMissingBody()
    {
        assertThrows(BadRequestException.class, () -> taskService.replaceEmployees(1L, null));
    }

    @Test
    @DisplayName("Replace employees - removing one employee keeps the others")
    void removingOneEmployeeKeepsTheOthers()
    {
        taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(1L, 2L)));

        TaskDTO result = taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(2L)));

        assertThat(result.assignedEmployees().size(), is(1));
        assertThat(result.assignedEmployees().get(0).id(), is(2L));
    }

    @Test
    @DisplayName("Replace employees - an invalid request leaves the existing assignments unchanged")
    void invalidRequestKeepsExistingAssignments()
    {
        taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(1L, 2L)));

        assertThrows(NotFoundException.class,
                () -> taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(1L, 99L))));

        List<Long> assignedIds = taskService.get(1L).assignedEmployees().stream()
                .map(EmployeeReferenceDTO::id)
                .toList();
        assertThat(assignedIds, is(List.of(1L, 2L)));
    }

    @Test
    @DisplayName("Replace employees - returns id, name and active status sorted by id")
    void assignedEmployeesAreReturnedWithReferenceFields()
    {
        TaskDTO result = taskService.replaceEmployees(1L, new TaskEmployeesDTO(List.of(2L, 1L)));

        EmployeeReferenceDTO first = result.assignedEmployees().get(0);
        assertThat(first.id(), is(1L));
        assertThat(first.firstName(), is("Alice"));
        assertThat(first.lastName(), is("Worker"));
        assertThat(first.active(), is(true));
        assertThat(result.assignedEmployees().get(1).id(), is(2L));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/task/task-service/remove-predecessor.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Remove predecessor - requires an existing dependency")
    void removePredecessorRequiresExistingDependency(Long taskId, Long predecessorId, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            taskService.removePredecessor(taskId, predecessorId);
            assertThat(taskService.get(taskId).predecessorIds(), not(hasItem(predecessorId)));
        }
        else
        {
            Exception exception = assertThrows(Exception.class,
                    () -> taskService.removePredecessor(taskId, predecessorId));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/task/task-service/get-scheduled-duration.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Scheduled duration - is the greater of the labor duration and the minimum duration")
    void calculatesScheduledDuration(double estimate, int minimumDurationInDays, double expected, String reason)
    {
        TaskDTO created = taskService.create(new TaskCreateDTO(1L, "Duration task", 1L, estimate, minimumDurationInDays));

        assertThat(taskService.get(created.id()).scheduledDurationInDays(), closeTo(expected, TOLERANCE));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/task/task-service/get-dependency-offsets.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Dependency offset - a task starts when its latest predecessor finishes")
    void calculatesDependencyOffsets(Integer firstPredecessorDays, Integer secondPredecessorDays, double expected,
                                     String reason)
    {
        Long taskId = createTask("Dependent task", 1);
        for (Integer predecessorDays : new Integer[] { firstPredecessorDays, secondPredecessorDays })
        {
            if (predecessorDays != null)
            {
                taskService.addPredecessor(taskId, createTask("Predecessor", predecessorDays));
            }
        }

        assertThat(taskService.get(taskId).dependencyStartOffsetInDays(), closeTo(expected, TOLERANCE));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/task/task-service/get-by-id.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Get - requires an existing task")
    void getRequiresExistingTask(Long id, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            assertThat(taskService.get(id).id(), is(id));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> taskService.get(id));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/task/task-service/delete-by-id.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Delete - requires an existing task")
    void deleteRequiresExistingTask(Long id, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            taskService.delete(id);
            assertThat(taskDAO.getStored(id), nullValue());
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> taskService.delete(id));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    private Long createTask(String name, int minimumDurationInDays)
    {
        return taskService.create(new TaskCreateDTO(1L, name, 1L, 0.0, minimumDurationInDays)).id();
    }

    private static Project project(Long id)
    {
        return Project.builder()
                .id(id)
                .title("Project " + id)
                .startDate(LocalDate.of(2030, 1, 7))
                .deadline(LocalDate.of(2030, 6, 28))
                .status(ProjectStatus.DRAFT)
                .build();
    }

    private List<Long> assignedEmployeeIds(Long taskId)
    {
        return taskService.get(taskId).assignedEmployees().stream()
                .map(EmployeeReferenceDTO::id)
                .toList();
    }

    private static List<Long> parseIds(String ids)
    {
        if (ids == null)
        {
            return null;
        }
        if (ids.equals("EMPTY"))
        {
            return List.of();
        }
        return Arrays.stream(ids.split(";")).map(Long::valueOf).toList();
    }

    /**
     * {@code TaskService} depends on the concrete {@code TaskDAO}, so this double extends it instead of implementing an
     * interface. It mirrors {@code TaskDAO}: a missing task raises {@link NotFoundException}, and deleting a task also
     * removes it as a predecessor of other tasks.
     */
    private static final class InMemoryTaskDAO implements ITaskDAO
    {
        private final Map<Long, Task> tasks = new LinkedHashMap<>();
        private long nextId = 1L;

        private InMemoryTaskDAO()
        {
        }

        @Override
        public Task create(Task task)
        {
            EntityIds.assign(task, nextId++);
            tasks.put(task.getId(), task);
            return task;
        }

        @Override
        public Task get(Long id)
        {
            Task task = tasks.get(id);
            if (task == null)
            {
                throw new NotFoundException("No task found with id: " + id);
            }
            return task;
        }

        @Override
        public List<Task> getAll()
        {
            return new ArrayList<>(tasks.values());
        }

        @Override
        public Task update(Task task)
        {
            get(task.getId());
            tasks.put(task.getId(), task);
            return task;
        }

        @Override
        public boolean delete(Long id)
        {
            Task deleted = get(id);
            tasks.values().forEach(task -> task.removePredecessor(deleted));
            return tasks.remove(id) != null;
        }

        private Task getStored(Long id)
        {
            return tasks.get(id);
        }
    }
}
