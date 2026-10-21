package app.dashboard.presentation;

import app.dashboard.domain.IDashboardService;
import app.dashboard.presentation.dto.DashboardSummaryDTO;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class DashboardController implements IDashboardController
{
    private final IDashboardService dashboardService;

    public DashboardController(IDashboardService dashboardService)
    {
        this.dashboardService = dashboardService;
    }

    @Override
    public void getSummary(Context ctx)
    {
        DashboardSummaryDTO summaryDTO = dashboardService.getSummary();
        ctx.status(HttpStatus.OK).json(summaryDTO);
    }
}
