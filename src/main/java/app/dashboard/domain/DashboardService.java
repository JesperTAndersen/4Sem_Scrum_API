package app.dashboard.domain;

import app.dashboard.presentation.DashboardSummaryDTO;
import app.project.data.IProjectDAO;
import app.project.domain.IScheduleService;
import app.project.domain.Project;

import java.math.BigDecimal;
import java.util.List;

public class DashboardService implements IDashboardService
{
    private IProjectDAO projectDAO;
    private IScheduleService scheduleService;

    public DashboardService(IProjectDAO projectDAO, IScheduleService scheduleService)
    {
        this.projectDAO = projectDAO;
        this.scheduleService = scheduleService;
    }

    @Override
    public DashboardSummaryDTO getSummary()
    {
        List<Project> projects = projectDAO.getAll();

        BigDecimal totalCosts = getTotalCost(projects);
        double totalHourse = getTotalHours(projects);

        return null;
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
