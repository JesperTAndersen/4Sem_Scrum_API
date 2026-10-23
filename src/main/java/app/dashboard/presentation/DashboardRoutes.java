package app.dashboard.presentation;

import app.security.domain.Role;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class DashboardRoutes
{
    private final IDashboardController dashboardController;

    public DashboardRoutes(IDashboardController dashboardController)
    {
        this.dashboardController = dashboardController;
    }

    public EndpointGroup getRoutes()
    {
        return () -> path("dashboard", () ->
        {
            get("/summary", dashboardController::getSummary, Role.PROJECT_MANAGER);
        });
    }
}
