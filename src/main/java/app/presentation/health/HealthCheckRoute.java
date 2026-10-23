package app.presentation.health;

import app.presentation.health.IHealthCheckController;
import app.security.domain.Role;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class HealthCheckRoute
{
    private final IHealthCheckController healthCheckController;

    public HealthCheckRoute(IHealthCheckController healthCheckController)
    {
        this.healthCheckController = healthCheckController;
    }

    public EndpointGroup getRoutes()
    {
        return () -> get("health", healthCheckController::healthCheck, Role.ANYONE);
    }
}
