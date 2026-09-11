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
                get("", userController::getAll);
                get("me", userController::getMe);
                get("{id}", userController::getById);
                put("{id}", userController::update);
                patch("{id}/role", userController::changeRole);
                patch("{id}/email", userController::changeEmail);
                patch("{id}/password", userController::changePassword);
                delete("{id}", userController::delete);
            });
    }
}
