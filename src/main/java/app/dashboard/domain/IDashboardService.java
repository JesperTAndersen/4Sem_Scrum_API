package app.dashboard.domain;

import app.dashboard.presentation.dto.DashboardSummaryDTO;

public interface IDashboardService
{
    public DashboardSummaryDTO getSummary();
}
