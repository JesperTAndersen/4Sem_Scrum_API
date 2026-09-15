package app.enums;

import io.javalin.security.RouteRole;

public enum Role implements RouteRole
{
    ANYONE,
    PROJECT_MANAGER, // Admin
    EMPLOYEE // User
}
