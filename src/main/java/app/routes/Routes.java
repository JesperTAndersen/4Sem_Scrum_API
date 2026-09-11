package app.routes;

import app.routes.CompetenceRoutes;
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
    private final UserRoutes userRoutes;
    private final CompetenceRoutes competenceRoutes;

    public Routes(HealthCheckRoute healthCheckRoute, UserRoutes userRoute, CompetenceRoutes competenceRoutes)
    {
        this.healthCheckRoute = healthCheckRoute;
        this.userRoutes = userRoute;
        this.competenceRoutes = competenceRoutes;
    }

    public EndpointGroup getRoutes()
    {
        return () ->
        {
            get("/", ctx -> ctx.status(200).json(Map.of("message", "Welcome to the Scrum API!")));

            path(API_VERSION, () ->
            {
                healthCheckRoute.getRoutes().addEndpoints();
                userRoutes.getRoutes().addEndpoints();
                competenceRoutes.getRoutes().addEndpoints();
            });
        };
    }
}
