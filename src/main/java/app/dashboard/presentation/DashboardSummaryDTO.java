package app.dashboard.presentation;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryDTO(
        ProjectStatisticsDTO projects,
        double totalEstimatedHours,
        BigDecimal totalCost,
        TaskStatisticsDTO tasks,
        CapacitySummaryDTO capacity,
        List<AtRiskProjectDTO> atRisk,
        List<UpcomingDeadlineDTO> upcomingDeadlines
)
{
}
