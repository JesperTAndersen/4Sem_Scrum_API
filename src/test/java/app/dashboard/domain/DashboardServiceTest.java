package app.dashboard.domain;

import app.competence.domain.Competence;
import app.dashboard.presentation.dto.AtRiskProjectDTO;
import app.dashboard.presentation.dto.DashboardSummaryDTO;
import app.dashboard.presentation.dto.UpcomingDeadlineDTO;
import app.employee.domain.Employee;
import app.employee.domain.EmployeeService;
import app.employee.presentation.dto.EmployeeCapacityDTO;
import app.persistence.testdoubles.InMemoryCompetenceDAO;
import app.persistence.testdoubles.InMemoryEmployeeDAO;
import app.persistence.testdoubles.InMemoryProjectDAO;
import app.persistence.testdoubles.InMemoryUserDAO;
import app.project.domain.Project;
import app.project.domain.ProjectService;
import app.project.domain.ProjectStatus;
import app.project.domain.ScheduleService;
import app.project.presentation.dto.ProjectDTO;
import app.stage.domain.Stage;
import app.task.domain.Task;
import app.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

class DashboardServiceTest
{
    // A five-day task starting Monday 2026-01-05 finishes Friday 2026-01-09.
    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 5);
    private static final LocalDate FINISH_DATE = LocalDate.of(2026, 1, 9);

    private InMemoryProjectDAO projectDAO;
    private InMemoryEmployeeDAO employeeDAO;
    private EmployeeService employeeService;
    private ProjectService projectService;
    private DashboardService dashboardService;
    private User managerUser;

    @BeforeEach
    void setUp()
    {
        InMemoryUserDAO userDAO = new InMemoryUserDAO();
        managerUser = userDAO.addUser("Project", "Manager", "manager@example.com", "Password1!");
        projectDAO = new InMemoryProjectDAO();
        employeeDAO = new InMemoryEmployeeDAO();

        // The company capacity DAO is only used when creating and updating employees.
        employeeService = new EmployeeService(employeeDAO, null, new InMemoryCompetenceDAO());
        projectService = new ProjectService(projectDAO, userDAO, new ScheduleService());
        dashboardService = new DashboardService(projectDAO, new ScheduleService(), employeeService);
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(resources = "/testcases/dashboard/dashboard-service/get-summary-project-inclusion.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Get summary - lists active projects by feasibility and leaves completed projects out")
    void getSummaryIncludesProjectsByStatusAndFeasibility(ProjectStatus status, String schedule,
                                                          boolean expectedAtRisk, boolean expectedUpcoming,
                                                          String reason)
    {
        LocalDate deadline = schedule.equals("INFEASIBLE") ? FINISH_DATE.minusDays(1) : FINISH_DATE;
        Project project = fiveDayProject(1L, status, deadline);
        if (schedule.equals("UNDETERMINED"))
        {
            unstaffableTask(project.getStages().iterator().next());
        }
        projectDAO.seed(project);

        DashboardSummaryDTO summary = dashboardService.getSummary();

        assertThat(summary.atRisk().size(), is(expectedAtRisk ? 1 : 0));
        assertThat(summary.projects().infeasible(), is(expectedAtRisk ? 1 : 0));
        assertThat(summary.upcomingDeadlines().size(), is(expectedUpcoming ? 1 : 0));
        if (expectedUpcoming)
        {
            UpcomingDeadlineDTO upcoming = summary.upcomingDeadlines().get(0);
            boolean undetermined = schedule.equals("UNDETERMINED");
            assertThat(upcoming.calculatedFinishDate(), is(undetermined ? null : FINISH_DATE));
            assertThat(upcoming.feasible(), is(undetermined ? null : schedule.equals("FEASIBLE")));
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/dashboard/dashboard-service/get-summary-upcoming-limit.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Get summary - returns at most five upcoming deadlines")
    void getSummaryLimitsUpcomingDeadlines(int activeProjects, int completedProjects, int expectedSize, String reason)
    {
        long id = 1L;
        // Completed projects get the earliest deadlines, so they would take the first places if they were included.
        for (int i = 0; i < completedProjects; i++)
        {
            projectDAO.seed(fiveDayProject(id++, ProjectStatus.COMPLETED, FINISH_DATE));
        }
        for (int i = 0; i < activeProjects; i++)
        {
            projectDAO.seed(fiveDayProject(id++, ProjectStatus.PLANNED, FINISH_DATE.plusDays(i + 1)));
        }

        assertThat(dashboardService.getSummary().upcomingDeadlines().size(), is(expectedSize));
    }

    @Test
    @DisplayName("Get summary - should order at-risk projects and upcoming deadlines by deadline")
    void getSummaryOrdersProjectsByDeadline()
    {
        projectDAO.seed(fiveDayProject(1L, ProjectStatus.PLANNED, FINISH_DATE.plusDays(10)));
        projectDAO.seed(fiveDayProject(2L, ProjectStatus.IN_PROGRESS, FINISH_DATE.minusDays(1)));
        projectDAO.seed(fiveDayProject(3L, ProjectStatus.DRAFT, FINISH_DATE.plusDays(5)));
        projectDAO.seed(fiveDayProject(4L, ProjectStatus.PLANNED, FINISH_DATE.minusDays(2)));

        DashboardSummaryDTO summary = dashboardService.getSummary();

        assertThat(summary.atRisk().stream().map(AtRiskProjectDTO::projectId).toList(), is(List.of(4L, 2L)));
        assertThat(summary.upcomingDeadlines().stream().map(UpcomingDeadlineDTO::projectId).toList(),
                is(List.of(4L, 2L, 3L, 1L)));
    }

    @Test
    @DisplayName("Get summary - should return zero values and empty lists for an empty database")
    void getSummaryWithoutDataReturnsZeroValues()
    {
        DashboardSummaryDTO summary = dashboardService.getSummary();

        assertAll(
                () -> assertThat(summary.projects().total(), is(0)),
                () -> assertThat(summary.projects().byStatus(), is(Map.of(
                        ProjectStatus.DRAFT, 0,
                        ProjectStatus.PLANNED, 0,
                        ProjectStatus.IN_PROGRESS, 0,
                        ProjectStatus.COMPLETED, 0))),
                () -> assertThat(summary.projects().infeasible(), is(0)),
                () -> assertThat(summary.totalEstimatedHours(), is(0.0)),
                () -> assertThat(summary.totalCost(), comparesEqualTo(BigDecimal.ZERO)),
                () -> assertThat(summary.tasks().notStarted(), is(0)),
                () -> assertThat(summary.tasks().inProgress(), is(0)),
                () -> assertThat(summary.tasks().done(), is(0)),
                () -> assertThat(summary.capacity().activeEmployees(), is(0)),
                () -> assertThat(summary.capacity().dailyCapacity(), is(0.0)),
                () -> assertThat(summary.atRisk(), is(empty())),
                () -> assertThat(summary.upcomingDeadlines(), is(empty()))
        );
    }

    @Test
    @DisplayName("Get summary - should match the totals, task counts and schedules of the project responses")
    void getSummaryMatchesProjectResponses()
    {
        Competence carpenter = new Competence("carpenter", new BigDecimal("850.00"));
        Competence painter = new Competence("painter", new BigDecimal("700.00"));

        Project kitchen = project(1L, ProjectStatus.IN_PROGRESS, FINISH_DATE.plusDays(30));
        Stage kitchenStage = stage(kitchen);
        task(kitchenStage, carpenter, 15.0, Task.TaskStatus.DONE);
        task(kitchenStage, painter, 7.5, Task.TaskStatus.IN_PROGRESS);

        Project villa = project(2L, ProjectStatus.DRAFT, START_DATE);
        task(stage(villa), carpenter, 30.0, Task.TaskStatus.NOT_STARTED);

        Project garage = project(3L, ProjectStatus.COMPLETED, FINISH_DATE.plusDays(30));
        task(stage(garage), painter, 7.5, Task.TaskStatus.DONE);

        projectDAO.seed(kitchen);
        projectDAO.seed(villa);
        projectDAO.seed(garage);

        DashboardSummaryDTO summary = dashboardService.getSummary();
        List<ProjectDTO> projects = List.of(projectService.get(1L), projectService.get(2L), projectService.get(3L));

        assertAll(
                () -> assertThat(summary.projects().total(), is(3)),
                () -> assertThat(summary.projects().byStatus(), is(Map.of(
                        ProjectStatus.DRAFT, 1,
                        ProjectStatus.PLANNED, 0,
                        ProjectStatus.IN_PROGRESS, 1,
                        ProjectStatus.COMPLETED, 1))),
                () -> assertThat(summary.totalEstimatedHours(),
                        is(projects.stream().mapToDouble(ProjectDTO::totalEstimatedHours).sum())),
                () -> assertThat(summary.totalCost(), comparesEqualTo(projects.stream()
                        .map(ProjectDTO::totalCost)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))),
                () -> assertThat(summary.tasks().notStarted(),
                        is(projects.stream().mapToInt(project -> project.tasks().tasksNotStarted()).sum())),
                () -> assertThat(summary.tasks().inProgress(),
                        is(projects.stream().mapToInt(project -> project.tasks().tasksInProgress()).sum())),
                () -> assertThat(summary.tasks().done(),
                        is(projects.stream().mapToInt(project -> project.tasks().taskDone()).sum()))
        );

        for (UpcomingDeadlineDTO upcoming : summary.upcomingDeadlines())
        {
            ProjectDTO project = projectService.get(upcoming.projectId());
            assertThat(upcoming.calculatedFinishDate(), is(project.schedule().calculatedFinishDate()));
            assertThat(upcoming.feasible(), is(project.schedule().feasible()));
        }
        assertThat(summary.atRisk().stream().map(AtRiskProjectDTO::projectId).toList(), is(List.of(2L)));
    }

    @Test
    @DisplayName("Get summary - should use the capacity of active employees from the employee capacity calculation")
    void getSummaryUsesActiveEmployeeCapacity()
    {
        employeeDAO.create(new Employee("Alice", "Worker", 7.5));
        employeeDAO.create(new Employee("Bob", "Builder", 7.5)).setActive(false);
        employeeDAO.create(new Employee("Carl", "Painter", 6.0));

        DashboardSummaryDTO summary = dashboardService.getSummary();
        EmployeeCapacityDTO capacity = employeeService.calculateDailyCapacity(null);

        assertAll(
                () -> assertThat(summary.capacity().activeEmployees(), is(2)),
                () -> assertThat(summary.capacity().dailyCapacity(), is(13.5)),
                () -> assertThat(summary.capacity().activeEmployees(), is(capacity.employeeCount())),
                () -> assertThat(summary.capacity().dailyCapacity(), is(capacity.dailyCapacity()))
        );
    }

    private Project fiveDayProject(Long id, ProjectStatus status, LocalDate deadline)
    {
        Project project = project(id, status, deadline);
        new Task(stage(project), "Build", 5);
        return project;
    }

    private Project project(Long id, ProjectStatus status, LocalDate deadline)
    {
        return Project.builder()
                .id(id)
                .title("Project " + id)
                .description("Project planning information")
                .startDate(START_DATE)
                .deadline(deadline)
                .status(status)
                .createdBy(managerUser)
                .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .updatedBy(managerUser)
                .updatedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .stages(new HashSet<>())
                .build();
    }

    private Stage stage(Project project)
    {
        Stage stage = new Stage("Stage", project);
        project.getStages().add(stage);
        return stage;
    }

    /**
     * A task whose only assigned employee is inactive has no available capacity, so its schedule is undetermined.
     */
    private void unstaffableTask(Stage stage)
    {
        Task task = new Task(stage, "No capacity", 0);
        task.setCompetence(null, 30.0);
        Employee inactive = new Employee("Inactive", "Employee", 7.5);
        inactive.setActive(false);
        task.assignEmployee(inactive);
    }

    private void task(Stage stage, Competence competence, double estimate, Task.TaskStatus status)
    {
        Task task = new Task(stage, "Task", 1);
        task.setCompetence(competence, estimate);
        task.changeStatus(status);
    }
}
