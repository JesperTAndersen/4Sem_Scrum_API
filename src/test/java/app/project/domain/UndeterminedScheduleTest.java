package app.project.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import app.employee.domain.Employee;
import app.persistence.testdoubles.EntityIds;
import app.project.presentation.dto.ScheduleDTO;
import app.stage.data.StageMapper;
import app.stage.domain.Stage;
import app.stage.presentation.dto.StageDTO;
import app.task.domain.Task;

class UndeterminedScheduleTest
{
    // 2030-01-07 is a Monday.
    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 7);
    private static final LocalDate FRIDAY = LocalDate.of(2030, 1, 4);

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/project/schedule-service/crew-reactivation.csv", numLinesToSkip = 1)
    @DisplayName("Crew changes - deactivation propagates and reactivation restores dependent dates")
    void crewChangesRestoreDependentSchedules(Task.TaskStatus status, String dependency, String reason)
    {
        Project project = project(MONDAY);
        Stage first = stage(project, "First");
        Stage next = dependency.equals("STAGE") ? stage(project, "Next") : first;
        Task work = EntityIds.assign(new Task(first, "Crew work", 0), 10L);
        work.setCompetence(null, 30.0);
        work.changeStatus(status);
        Employee firstWorker = EntityIds.assign(new Employee("First", "Worker", 7.5), 1L);
        Employee secondWorker = EntityIds.assign(new Employee("Second", "Worker", 7.5), 2L);
        // Reverse insertion order verifies warnings are returned in employee ID order.
        work.assignEmployee(secondWorker);
        work.assignEmployee(firstWorker);
        Task successor = staffedTask(next, "Successor", 7.5, 7.5);
        if (dependency.equals("STAGE")) next.getPredecessors().add(first);
        else successor.addPredecessor(work);

        ScheduleService service = new ScheduleService();
        assertThat(service.calculateFinishDate(project).calculatedFinishDate(), is(MONDAY.plusDays(2)));
        firstWorker.setActive(false);
        assertThat(work.getLaborDurationInDays(), is(4.0));
        assertThat(WorkingDaySchedule.calculateTaskDates(successor, MONDAY).startDate(), is(MONDAY.plusDays(4)));
        assertThat(service.calculateFinishDate(project).staffingWarnings().size(), is(1));

        secondWorker.setActive(false);
        ScheduleDTO unavailable = service.calculateFinishDate(project);
        assertThat(unavailable.calculatedFinishDate(), is(nullValue()));
        assertThat(unavailable.feasible(), is(nullValue()));
        assertThat(WorkingDaySchedule.calculateTaskDates(successor, MONDAY).isDetermined(), is(false));
        assertThat(unavailable.staffingWarnings().stream().map(w -> w.employeeId()).toList(),
                is(List.of(1L, 2L)));

        firstWorker.setActive(true);
        assertThat(service.calculateFinishDate(project).calculatedFinishDate(), is(MONDAY.plusDays(4)));
        secondWorker.setActive(true);
        ScheduleDTO restored = service.calculateFinishDate(project);
        assertThat(restored.calculatedFinishDate(), is(MONDAY.plusDays(2)));
        assertThat(restored.feasible(), is(true));
        assertThat(restored.staffingWarnings().isEmpty(), is(true));
        assertThat(work.getEmployees().size(), is(2));
    }

    @Test
    @DisplayName("Task dates - undetermined when the task has no available capacity")
    void taskWithoutAvailableCapacityHasNoDates()
    {
        Stage stage = stage(project(MONDAY), "Stage");
        Task task = unstaffableTask(stage, "No capacity");

        WorkingDaySchedule.TaskDates dates = WorkingDaySchedule.calculateTaskDates(task, MONDAY);

        assertThat(dates.isDetermined(), is(false));
        assertThat(dates.startDate(), is(nullValue()));
        assertThat(dates.endDate(), is(nullValue()));
    }

    @Test
    @DisplayName("Task dates - a successor of an undetermined task is undetermined")
    void successorOfUndeterminedTaskIsUndetermined()
    {
        Stage stage = stage(project(MONDAY), "Stage");
        Task predecessor = unstaffableTask(stage, "No capacity");
        Task successor = staffedTask(stage, "Successor", 7.5, 7.5);
        successor.addPredecessor(predecessor);

        assertThat(WorkingDaySchedule.calculateTaskDates(successor, MONDAY).isDetermined(), is(false));
    }

    @Test
    @DisplayName("Task dates - tasks in a dependent stage are undetermined when a predecessor stage is undetermined")
    void dependentStageIsUndeterminedAfterUndeterminedPredecessorStage()
    {
        Project project = project(MONDAY);
        Stage first = stage(project, "First");
        Stage next = stage(project, "Next");
        next.getPredecessors().add(first);
        unstaffableTask(first, "No capacity");
        Task work = staffedTask(next, "Work", 7.5, 7.5);

        assertThat(WorkingDaySchedule.calculateTaskDates(work, MONDAY).isDetermined(), is(false));
    }

    @Test
    @DisplayName("Task dates - a staffed task skips the weekend")
    void staffedTaskSkipsWeekend()
    {
        Stage stage = stage(project(FRIDAY), "Stage");
        Task task = staffedTask(stage, "Two days", 15.0, 7.5);

        WorkingDaySchedule.TaskDates dates = WorkingDaySchedule.calculateTaskDates(task, FRIDAY);

        assertThat(dates.startDate(), is(FRIDAY));
        assertThat(dates.endDate(), is(MONDAY));
    }

    @Test
    @DisplayName("Stage dates - start is the earliest known start and end is undetermined")
    void stageWithUndeterminedTaskHasNoEndDate()
    {
        Stage stage = stage(project(MONDAY), "Stage");
        staffedTask(stage, "Known", 7.5, 7.5);
        unstaffableTask(stage, "No capacity");

        StageDTO dto = StageMapper.toDTO(stage);

        assertThat(dto.startDate(), is(MONDAY));
        assertThat(dto.endDate(), is(nullValue()));
    }

    @Test
    @DisplayName("Project schedule - finish date and feasibility are undetermined when a task is undetermined")
    void projectWithUndeterminedTaskHasUndeterminedSchedule()
    {
        Project project = project(MONDAY);
        Stage stage = stage(project, "Stage");
        staffedTask(stage, "Known", 7.5, 7.5);
        unstaffableTask(stage, "No capacity");

        ScheduleDTO schedule = new ScheduleService().calculateFinishDate(project);

        assertThat(schedule.calculatedFinishDate(), is(nullValue()));
        assertThat(schedule.feasible(), is(nullValue()));
    }

    @Test
    @DisplayName("Project schedule - a fully staffed project is still calculated as before")
    void staffedProjectHasDeterminedSchedule()
    {
        Project project = project(MONDAY);
        staffedTask(stage(project, "Stage"), "Known", 15.0, 7.5);

        ScheduleDTO schedule = new ScheduleService().calculateFinishDate(project);

        assertThat(schedule.calculatedFinishDate(), is(MONDAY.plusDays(1)));
        assertThat(schedule.feasible(), is(true));
    }

    private static Project project(LocalDate startDate)
    {
        return Project.builder()
                .id(1L)
                .startDate(startDate)
                .deadline(startDate.plusYears(1))
                .stages(new HashSet<>())
                .build();
    }

    private static Stage stage(Project project, String name)
    {
        Stage stage = new Stage(name, project);
        project.getStages().add(stage);
        return stage;
    }

    private static Task staffedTask(Stage stage, String name, double estimate, double dailyCapacity)
    {
        Task task = new Task(stage, name, 0);
        task.setCompetence(null, estimate);
        task.assignEmployee(new Employee("Active", "Employee", dailyCapacity));
        return task;
    }

    private static Task unstaffableTask(Stage stage, String name)
    {
        Task task = new Task(stage, name, 0);
        task.setCompetence(null, 30.0);
        Employee inactive = new Employee("Inactive", "Employee", 7.5);
        inactive.setActive(false);
        task.assignEmployee(inactive);
        return task;
    }
}
