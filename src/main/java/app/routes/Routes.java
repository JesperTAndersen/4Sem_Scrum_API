package app.routes;

import io.javalin.apibuilder.EndpointGroup;
import lombok.Getter;

import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

public class Routes
{
    @Getter
    private static final String API_VERSION = "api/v1";
    private final HealthCheckRoute healthCheckRoute;

    public Routes(
            HealthCheckRoute healthCheckRoute)
    {
        this.healthCheckRoute = healthCheckRoute;
    }

    public EndpointGroup getRoutes()
    {
        return () ->
        {
            get("/", ctx -> ctx.status(200).json(Map.of("message", "Welcome to the Scrum API!")));

            path(API_VERSION, () ->
            {
                healthCheckRoute.getRoutes().addEndpoints();
            });
        };
    }
}