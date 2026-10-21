package app.dashboard.domain;

import app.dashboard.presentation.dto.*;
import app.employee.domain.IEmployeeService;
import app.employee.presentation.dto.EmployeeCapacityDTO;
import app.project.data.IProjectDAO;
import app.project.data.ProjectMapper;
import app.project.domain.IScheduleService;
import app.project.domain.Project;
import app.project.domain.ProjectStatus;
import app.project.presentation.dto.ScheduleDTO;
import app.task.domain.Task;
import app.task.presentation.dto.TaskCountDTO;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DashboardService implements IDashboardService
{
    private record ScheduledProject(Project project, ScheduleDTO scheduleDTO)
    {
    }

    private final IProjectDAO projectDAO;
    private final IScheduleService scheduleService;
    private final IEmployeeService employeeService;

    public DashboardService(IProjectDAO projectDAO, IScheduleService scheduleService, IEmployeeService employeeService)
    {
        this.projectDAO = projectDAO;
        this.scheduleService = scheduleService;
        this.employeeService = employeeService;
    }

    @Override
    public DashboardSummaryDTO getSummary()
    {
        List<Project> projects = projectDAO.getAll();
        List<Project> activeProjects = getActiveProjects(projects);
        List<ScheduledProject> activeProjectsByDeadline = getProjectsByDeadline(activeProjects);
        List<AtRiskProjectDTO> atRisk = getAtRisk(activeProjectsByDeadline);

        return new DashboardSummaryDTO(
                getProjectStatistics(projects, atRisk.size()),
                getTotalHours(activeProjects),
                getTotalCost(activeProjects),
                getRemainingHours(activeProjects),
                getTaskStatistics(projects),
                getCapacity(),
                atRisk,
                getUpcomingDeadlines(activeProjectsByDeadline)
        );
    }

    private List<Project> getActiveProjects(List<Project> projects)
    {
        return projects.stream()
                .filter(project -> project.getStatus() != ProjectStatus.COMPLETED)
                .toList();
    }

    private ProjectStatisticsDTO getProjectStatistics(List<Project> projects, int infeasible)
    {
        Map<ProjectStatus, Integer> byStatus = new EnumMap<>(ProjectStatus.class);
        for (ProjectStatus status : ProjectStatus.values())
        {
            byStatus.put(status, 0);
        }

        for (Project project : projects)
        {
            ProjectStatus status = project.getStatus();
            byStatus.put(status, byStatus.get(status) + 1);
        }

        return new ProjectStatisticsDTO(projects.size(), byStatus, infeasible);
    }

    private BigDecimal getTotalCost(List<Project> projects)
    {
        return projects.stream()
                .map(Project::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double getTotalHours(List<Project> projects)
    {
        return projects.stream()
                .mapToDouble(Project::getTotalEstimatedHours)
                .sum();
    }

    private double getRemainingHours(List<Project> projects)
    {
        return projects.stream()
                .flatMap(project -> project.getStages().stream())
                .flatMap(stage -> stage.getTasks().stream())
                .filter(task -> task.getStatus() != Task.TaskStatus.DONE)
                .mapToDouble(Task::getEstimate)
                .sum();
    }

    private List<ScheduledProject> getProjectsByDeadline(List<Project> projects)
    {
        return projects.stream()
                .map(project -> new ScheduledProject(project,
                        scheduleService.calculateFinishDate(project)))
                .sorted(Comparator.comparing((ScheduledProject scheduled) -> scheduled.project().getDeadline())
                        .thenComparing(scheduled -> scheduled.project().getId()))
                .toList();
    }

    private List<AtRiskProjectDTO> getAtRisk(List<ScheduledProject> activeProjects)
    {
        return activeProjects.stream()
                // An undetermined schedule has feasible == null and cannot be evaluated, so it is not at risk.
                .filter(scheduled -> Boolean.FALSE.equals(scheduled.scheduleDTO().feasible()))
                .map(scheduled -> new AtRiskProjectDTO(
                        scheduled.project().getId(),
                        scheduled.project().getTitle(),
                        scheduled.project().getDeadline(),
                        scheduled.scheduleDTO().calculatedFinishDate()
                ))
                .toList();
    }

    private List<UpcomingDeadlineDTO> getUpcomingDeadlines(List<ScheduledProject> activeProjects)
    {
        return activeProjects.stream()
                .limit(5)
                .map(scheduled -> new UpcomingDeadlineDTO(
                        scheduled.project().getId(),
                        scheduled.project().getTitle(),
                        scheduled.project().getDeadline(),
                        scheduled.scheduleDTO().calculatedFinishDate(),
                        scheduled.scheduleDTO().feasible()
                ))
                .toList();
    }

    private TaskStatisticsDTO getTaskStatistics(List<Project> projects)
    {
        int notStarted = 0;
        int inProgress = 0;
        int done = 0;

        for (Project project : projects)
        {
            TaskCountDTO taskCount = ProjectMapper.buildTaskCountDTO(project);
            notStarted += taskCount.tasksNotStarted();
            inProgress += taskCount.tasksInProgress();
            done += taskCount.taskDone();
        }

        return new TaskStatisticsDTO(notStarted, inProgress, done);
    }

    private CapacitySummaryDTO getCapacity()
    {
        EmployeeCapacityDTO capacity = employeeService.calculateDailyCapacity(null);
        return new CapacitySummaryDTO(
                capacity.employeeCount(),
                capacity.dailyCapacity()
        );
    }
}
