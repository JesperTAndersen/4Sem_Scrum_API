package app.dashboard.presentation;

import io.javalin.apibuilder.EndpointGroup;
import static io.javalin.apibuilder.ApiBuilder.*;

public class DashboardRoutes
{
   private final DashboardController dashboardController;

    public DashboardRoutes(DashboardController dashboardController)
    {
        this.dashboardController = dashboardController;
    }

    public EndpointGroup getRoutes()
    {
        return () -> {
            get(dashboardController::getSummary);
        };
    }
}
