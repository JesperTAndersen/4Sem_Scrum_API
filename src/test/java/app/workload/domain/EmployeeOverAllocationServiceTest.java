package app.workload.domain;

import app.capacity.domain.CompanyCapacity;
import app.competence.domain.Competence;
import app.employee.domain.Employee;
import app.exceptions.ApiException;
import app.persistence.testdoubles.EntityIds;
import app.persistence.testdoubles.InMemoryTaskReader;
import app.persistence.testdoubles.InMemoryEmployeeDAO;
import app.workload.presentation.dto.TaskHoursDTO;
import app.workload.presentation.dto.UnassessedTaskDTO;
import app.project.domain.Project;
import app.stage.domain.Stage;
import app.task.domain.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmployeeOverAllocationServiceTest
{
    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 7);
    private final InMemoryTaskReader tasks = new InMemoryTaskReader();
    private final InMemoryEmployeeDAO employees = new InMemoryEmployeeDAO();
    private final EmployeeWorkloadService service = new EmployeeWorkloadService(tasks, employees);
    private long nextTaskId = 1;

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/planning/employee-over-allocation-service/calculate-range.csv",
            numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Find conflicts - validates inclusive dates and returns empty results without assignments")
    void validatesRange(LocalDate from, LocalDate to, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            var result = service.findConflicts(from, to);
            assertEquals(from, result.startDate());
            assertEquals(to, result.endDate());
            assertTrue(result.conflicts().isEmpty());
            assertTrue(result.unassessedTaskIds().isEmpty());
        }
        else
        {
            ApiException exception = assertThrows(ApiException.class, () -> service.findConflicts(from, to));
            assertEquals(expected, exception.getClass().getSimpleName());
        }
    }

    @ParameterizedTest(name = "[{index}] {9}")
    @CsvFileSource(resources = "/testcases/planning/employee-over-allocation-service/calculate-shares.csv",
            numLinesToSkip = 1)
    @DisplayName("Find conflicts - attributes conflicts using capacity shares and scheduled working days")
    void calculatesShares(double estimate, double firstCapacity, double secondCapacity, int minimumDays,
                          LocalDate start, LocalDate from, LocalDate to, double firstHours, double secondHours,
                          String reason)
    {
        Employee first = employee(firstCapacity);
        Employee second = employee(secondCapacity);
        Task work = task(project(1, start), estimate, minimumDays, first, second);
        // Both employees already have a full working day on another project.
        Project other = project(2, from);
        task(other, firstCapacity * 20, 0, first);
        task(other, secondCapacity * 20, 0, second);

        var result = service.findConflicts(from, to);
        double firstDemand = 0;
        double secondDemand = 0;
        for (var conflict : result.conflicts())
        {
            assertTrue(conflict.date().getDayOfWeek().getValue() <= 5);
            for (TaskHoursDTO taskHours : conflict.taskHours())
            {
                if (!taskHours.taskId().equals(work.getId()))
                {
                    continue;
                }
                if (conflict.employeeId().equals(first.getId()))
                {
                    firstDemand += taskHours.allocatedHours();
                }
                else
                {
                    secondDemand += taskHours.allocatedHours();
                }
            }
        }
        assertEquals(firstHours, firstDemand, 0.000000001);
        assertEquals(secondHours, secondDemand, 0.000000001);
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(resources = "/testcases/planning/employee-over-allocation-service/calculate-overlap.csv",
            numLinesToSkip = 1)
    @DisplayName("Find conflicts - reports daily cross-project conflicts without averaging spare days")
    void calculatesOverlap(LocalDate secondStart, double secondEstimate, int conflictCount,
                           double expectedExcess, String reason)
    {
        Employee shared = employee(7.5);
        Employee unrelated = employee(7.5);
        task(project(1, MONDAY), 7.5, 1, shared);
        task(project(2, secondStart), secondEstimate, 1, shared);
        task(project(3, MONDAY), 7.5, 1, unrelated);
        var result = service.findConflicts(MONDAY, MONDAY.plusDays(4));
        assertEquals(conflictCount, result.conflicts().size());
        if (conflictCount > 0)
        {
            var conflict = result.conflicts().getFirst();
            assertEquals(shared.getId(), conflict.employeeId());
            assertEquals("Employee", conflict.firstName());
            assertEquals(MONDAY, conflict.date());
            assertEquals(7.5, conflict.availableHours());
            assertEquals(10, conflict.allocatedHours());
            assertEquals(expectedExcess, conflict.excessHours());
            assertEquals(List.of(1L, 2L), conflict.taskHours().stream()
                    .map(TaskHoursDTO::projectId).toList());
        }
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(resources = "/testcases/planning/employee-over-allocation-service/calculate-staffing.csv",
            numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Find conflicts - lists unassessed tasks and excludes completed or zero-effort demand")
    void reportsUnassessedTasks(String scenario, int unassessedCount, int conflictCount, String expectedReason,
                                String reason)
    {
        Employee first = employee(7.5);
        Employee second = employee(2.5);
        Project project = project(1, MONDAY);
        Task work = task(project, 30, 0, first, second);
        switch (scenario)
        {
            case "PARTIAL_INACTIVE" -> first.setActive(false);
            case "ALL_INACTIVE" ->
            {
                first.setActive(false);
                second.setActive(false);
            }
            case "UNASSIGNED" -> work.replaceEmployees(List.of());
            case "INELIGIBLE" -> work.setCompetence(new Competence("Required", BigDecimal.ONE), 30);
            case "DONE", "DONE_LEGACY" ->
            {
                work.changeStatus(Task.TaskStatus.DONE);
                if (scenario.equals("DONE"))
                {
                    work.recordPlanningSnapshot(MONDAY, MONDAY.plusDays(2), 3.0, 3.0);
                }
            }
            case "IN_PROGRESS" -> work.changeStatus(Task.TaskStatus.IN_PROGRESS);
            case "ZERO" -> work.setCompetence(null, 0);
            case "BLOCKED_SUCCESSOR" ->
            {
                Task predecessor = task(project, 7.5, 0, first);
                first.setActive(false);
                work.replaceEmployees(List.of(second));
                work.addPredecessor(predecessor);
            }
            default -> throw new AssertionError("Unknown scenario: " + scenario);
        }
        if (work.getStatus() == Task.TaskStatus.DONE || work.getStatus() == Task.TaskStatus.IN_PROGRESS
                || scenario.equals("ZERO"))
        {
            task(project(2, MONDAY), 7.5 * 20, 0, first);
        }
        var result = service.findConflicts(MONDAY, MONDAY.plusDays(4));
        assertEquals(unassessedCount, result.unassessedTaskIds().size());
        assertEquals(result.unassessedTaskIds(), result.unassessedTasks().stream()
                .map(UnassessedTaskDTO::taskId).toList());
        if (unassessedCount > 0)
        {
            assertTrue(result.unassessedTaskIds().contains(work.getId()));
            UnassessedTaskDTO unassessed = result.unassessedTasks().stream()
                    .filter(task -> task.taskId().equals(work.getId()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(expectedReason, unassessed.reason());
            assertEquals(work.getName(), unassessed.taskName());
            assertEquals(project.getId(), unassessed.projectId());
            assertEquals(project.getTitle(), unassessed.projectTitle());
        }
        assertEquals(conflictCount, result.conflicts().size());
        if (conflictCount > 0)
        {
            assertEquals(7.5, result.conflicts().getFirst().excessHours());
        }
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Find conflicts - task hours include the competence the task requires")
    void includesCompetenceInTaskHours()
    {
        Competence carpenter = EntityIds.assign(new Competence("Carpenter", BigDecimal.ONE), 1L);
        Employee shared = employee(7.5);
        shared.addCompetence(carpenter);
        task(project(1, MONDAY), 7.5, 1, shared).setCompetence(carpenter, 7.5);
        task(project(2, MONDAY), 7.5, 1, shared);

        var conflict = service.findConflicts(MONDAY, MONDAY).conflicts().getFirst();

        TaskHoursDTO withCompetence = conflict.taskHours().getFirst();
        assertEquals(carpenter.getId(), withCompetence.competenceId());
        assertEquals("Carpenter", withCompetence.competenceName());
        TaskHoursDTO withoutCompetence = conflict.taskHours().get(1);
        assertEquals(null, withoutCompetence.competenceId());
        assertEquals(null, withoutCompetence.competenceName());
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(resources = "/testcases/planning/employee-over-allocation-service/calculate-precision.csv",
            numLinesToSkip = 1)
    @DisplayName("Find conflicts - ignores only differences below the agreed capacity tolerance")
    void comparesPrecision(double dailyCapacity, double extraHours, boolean overAllocated,
                           double excess, String reason)
    {
        Employee employee = employee(dailyCapacity);
        task(project(1, MONDAY), dailyCapacity, 1, employee);
        task(project(2, MONDAY), extraHours, 1, employee);
        var result = service.findConflicts(MONDAY, MONDAY);
        assertEquals(overAllocated ? 1 : 0, result.conflicts().size());
        if (overAllocated)
        {
            assertEquals(excess, result.conflicts().getFirst().excessHours(), 0.000000001);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/planning/employee-over-allocation-service/calculate-crew-changes.csv",
            numLinesToSkip = 1)
    @DisplayName("Find conflicts - uses unique employee identities and current effective capacity")
    void recalculatesCrew(String change, double expectedDailyHours, String reason)
    {
        Employee first = employee(7.5);
        Employee second = employee(2.5);
        Task work = task(project(1, MONDAY), 30, 0, first, second);
        Task other = task(project(2, MONDAY), 7.5, 0, first);
        switch (change)
        {
            case "STANDARD_CAPACITY" ->
            {
                CompanyCapacity company = new CompanyCapacity();
                first.configureCapacity(true, company);
                company.update(2.5);
            }
            case "REACTIVATION" ->
            {
                first.setActive(false);
                assertTrue(service.findConflicts(MONDAY, MONDAY).unassessedTaskIds().contains(other.getId()));
                first.setActive(true);
            }
            case "DUPLICATE_IDENTITY" -> work.assignEmployee(
                    EntityIds.assign(new Employee("Duplicate", "Identity", 7.5), first.getId()));
            case "MULTIPLE_COMPETENCES" ->
            {
                Competence required = EntityIds.assign(new Competence("Required", BigDecimal.ONE), 1L);
                first.addCompetence(required);
                first.addCompetence(EntityIds.assign(new Competence("Other", BigDecimal.ONE), 2L));
                second.addCompetence(required);
                work.setCompetence(required, 30);
            }
            case "ASSIGNMENT_REMOVED" -> work.unassignEmployee(second);
            default -> throw new AssertionError("Unknown change: " + change);
        }
        var result = service.findConflicts(MONDAY, MONDAY);
        assertEquals(1, result.conflicts().size());
        var conflict = result.conflicts().getFirst();
        assertEquals(first.getId(), conflict.employeeId());
        var taskHours = conflict.taskHours().stream()
                .filter(task -> work.getId().equals(task.taskId())).findFirst().orElseThrow();
        assertEquals(expectedDailyHours, taskHours.allocatedHours(), 0.000000001);
        assertTrue(result.unassessedTaskIds().isEmpty());
    }

    @ParameterizedTest(name = "[{index}] {5}")
    @CsvFileSource(resources = "/testcases/planning/employee-over-allocation-service/calculate-unassessed-period.csv",
            numLinesToSkip = 1)
    @DisplayName("Find conflicts - retains unknown dates without reporting known out-of-period tasks")
    void scopesUnassessedTasks(boolean inactive, LocalDate start, LocalDate from, LocalDate to,
                              int unassessedCount, String reason)
    {
        Task task = task(project(1, start), 7.5, 0);
        if (inactive)
        {
            Employee employee = employee(7.5);
            employee.setActive(false);
            task.assignEmployee(employee);
        }
        var result = service.findConflicts(from, to);
        assertEquals(unassessedCount, result.unassessedTaskIds().size());
    }

    @ParameterizedTest(name = "[{index}] {5}")
    @CsvFileSource(resources = "/testcases/planning/employee-over-allocation-service/calculate-availability.csv",
            numLinesToSkip = 1)
    @DisplayName("Find availability - includes idle employees and shares the daily workload rules")
    void calculatesAvailability(String scenario, int employeeCount, double allocatedHours,
                               double remainingHours, int unassessedCount, String reason)
    {
        Employee employee = employee(7.5);
        LocalDate date = MONDAY;
        switch (scenario)
        {
            case "IDLE" -> { }
            case "PARTIAL" -> task(project(1, MONDAY), 12.5, 5, employee);
            case "FULL" -> task(project(1, MONDAY), 37.5, 5, employee);
            case "CROSS_PROJECT" ->
            {
                task(project(1, MONDAY), 37.5, 5, employee);
                task(project(2, MONDAY), 12.5, 5, employee);
            }
            case "INACTIVE" -> employee.setActive(false);
            case "SATURDAY" -> date = MONDAY.plusDays(5);
            case "SUNDAY" -> date = MONDAY.plusDays(6);
            case "STANDARD_CAPACITY" ->
            {
                CompanyCapacity company = new CompanyCapacity();
                company.update(10);
                employee.configureCapacity(true, company);
                task(project(1, MONDAY), 12.5, 5, employee);
            }
            case "UNASSESSED" -> task(project(1, MONDAY), 12.5, 5);
            case "DONE" -> task(project(1, MONDAY), 12.5, 5, employee).changeStatus(Task.TaskStatus.DONE);
            case "ZERO_ESTIMATE" -> task(project(1, MONDAY), 0, 5, employee);
            case "OUTSIDE_DATES" -> task(project(1, MONDAY.plusDays(7)), 12.5, 5, employee);
            default -> throw new AssertionError("Unknown scenario: " + scenario);
        }

        var result = service.findAvailability(date, null);
        assertEquals(date, result.date());
        assertEquals(employeeCount, result.employees().size());
        assertEquals(unassessedCount, result.unassessedTaskIds().size());
        if (employeeCount > 0)
        {
            var availability = result.employees().getFirst();
            assertEquals(employee.getId(), availability.employeeId());
            assertEquals(employee.getFirstName(), availability.firstName());
            assertEquals(employee.getLastName(), availability.lastName());
            assertEquals(employee.getDailyCapacity(), availability.dailyCapacity());
            assertEquals(allocatedHours, availability.allocatedHours(), 0.000000001);
            assertEquals(remainingHours, availability.remainingHours(), 0.000000001);
        }
        if (unassessedCount > 0)
        {
            assertEquals(List.of(1L), result.unassessedTaskIds());
        }
    }

    @org.junit.jupiter.api.Test
    void rejectsMissingAvailabilityDate()
    {
        assertThrows(app.exceptions.BadRequestException.class, () -> service.findAvailability(null, null));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/planning/employee-over-allocation-service/filter-availability.csv",
            numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Find availability - filters employees while counting work requiring other competences")
    void filtersAvailability(Long competenceId, int employeeCount, String reason)
    {
        Competence required = EntityIds.assign(new Competence("Required", BigDecimal.ONE), 1L);
        Competence other = EntityIds.assign(new Competence("Other", BigDecimal.ONE), 2L);
        Employee matching = employee(7.5);
        matching.addCompetence(required);
        matching.addCompetence(other);
        Employee idle = employee(7.5);
        idle.addCompetence(required);
        Employee nonMatching = employee(7.5);
        nonMatching.addCompetence(other);
        Employee inactive = employee(7.5);
        inactive.addCompetence(required);
        inactive.setActive(false);
        Task work = task(project(1, MONDAY), 12.5, 5, matching);
        work.setCompetence(other, 12.5);

        if (competenceId != null && competenceId <= 0)
        {
            assertThrows(app.exceptions.BadRequestException.class,
                    () -> service.findAvailability(MONDAY, competenceId));
            return;
        }

        var result = service.findAvailability(MONDAY, competenceId);
        assertEquals(employeeCount, result.employees().size());
        if (employeeCount > 0)
        {
            var first = result.employees().getFirst();
            assertEquals(matching.getId(), first.employeeId());
            assertEquals(2.5, first.allocatedHours(), 0.000000001);
            assertEquals(5, first.remainingHours(), 0.000000001);
        }
        if (Long.valueOf(1).equals(competenceId))
        {
            assertEquals(idle.getId(), result.employees().get(1).employeeId());
            assertEquals(7.5, result.employees().get(1).remainingHours());
        }
    }

    private Project project(long id, LocalDate start)
    {
        Project project = Project.builder().id(id).title("Project " + id).startDate(start)
                .deadline(start.plusDays(1)).build();
        return project;
    }

    private Task task(Project project, double estimate, int minimumDays, Employee... crew)
    {
        Stage stage = new Stage("Stage", project);
        project.getStages().add(stage);
        Task task = EntityIds.assign(new Task(stage, "Task", minimumDays), nextTaskId++);
        task.setCompetence(null, estimate);
        task.replaceEmployees(List.of(crew));
        tasks.seed(task);
        return task;
    }

    private Employee employee(double capacity)
    {
        return employees.create(new Employee("Employee", "Name", capacity));
    }
}
