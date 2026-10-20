package app.dashboard.domain;

import app.dashboard.presentation.dto.DashboardSummaryDTO;
import app.dashboard.presentation.dto.ProjectStatisticsDTO;
import app.employee.domain.IEmployeeService;
import app.project.data.IProjectDAO;
import app.project.domain.IScheduleService;
import app.project.domain.Project;
import app.project.domain.ProjectStatus;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DashboardService implements IDashboardService
{
    private IProjectDAO projectDAO;
    private IScheduleService scheduleService;
    private IEmployeeService employeeService;

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

        BigDecimal totalCosts = getTotalCost(projects);
        double totalHours = getTotalHours(projects);

        return null;
    }

    private ProjectStatisticsDTO getProjectStatistics(List<Project> projects, int infeasible)
    {
        Map<ProjectStatus, Integer> byStatus = new EnumMap<>(ProjectStatus.class);

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
}
