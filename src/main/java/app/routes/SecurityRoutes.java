package app.routes;

import app.controllers.interfaces.generic.ISecurityController;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class SecurityRoutes
{
    private final ISecurityController securityController;

    public SecurityRoutes(ISecurityController securityController)
    {
        this.securityController = securityController;
    }

    public EndpointGroup getRoutes()
    {
        return () -> path("auth", () ->
        {

        });
    }
}