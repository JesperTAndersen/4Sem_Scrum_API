package app.dashboard.presentation.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryDTO(
        ProjectStatisticsDTO projects,
        double totalEstimatedHours,
        BigDecimal totalCost,
        double remainingHours,
        TaskStatisticsDTO tasks,
        CapacitySummaryDTO capacity,
        List<AtRiskProjectDTO> atRisk,
        List<UpcomingDeadlineDTO> upcomingDeadlines
)
{
}
