package app.security.presentation;

import app.security.presentation.ISecurityController;
import app.security.domain.Role;
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
            post("/register", securityController::register, Role.ANYONE);
            post("/login", securityController::login, Role.ANYONE);
        });
    }
}