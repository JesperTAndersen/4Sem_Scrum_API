package app.controllers.routes;

import app.controllers.IUserController;
import app.enums.Role;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class UserRoute
{
    private final IUserController userController;

    public UserRoute(IUserController userController)
    {
        this.userController = userController;
    }

    public EndpointGroup getRoutes()
    {
        return () ->
            path("users", () ->
            {
                get("", userController::getAll, Role.ADMIN);
                get("me", userController::getMe, Role.PROJECT_MANAGER, Role.ADMIN, Role.ANYONE);
                get("{id}", userController::getById, Role.PROJECT_MANAGER, Role.ADMIN);
                put("{id}", userController::update, Role.PROJECT_MANAGER);
                patch("{id}/role", userController::changeRole, Role.ADMIN);
                patch("{id}/email", userController::changeEmail, Role.PROJECT_MANAGER, Role.ADMIN, Role.ANYONE);
                patch("{id}/password", userController::changePassword, Role.PROJECT_MANAGER, Role.ADMIN, Role.ANYONE);
                delete("{id}", userController::delete, Role.ADMIN);
            });
    }
}
