package app.controllers.routes;

import app.controllers.SecurityController;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class SecurityRoutes
{
    private final SecurityController securityController;

    public SecurityRoutes(SecurityController securityController)
    {
        this.securityController = securityController;
    }

    public EndpointGroup getRoutes()
    {
        return () -> path("auth", () ->
        {
            get("/healthcheck", securityController::healthCheck);
        });
    }
}