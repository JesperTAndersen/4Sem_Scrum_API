package app.user.presentation;

import app.user.presentation.IUserController;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class UserRoutes
{
    private final IUserController userController;

    public UserRoutes(IUserController userController)
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
                get("{id}", userController::get);
                put("{id}", userController::update);
                patch("{id}/role", userController::changeRole);
                patch("{id}/email", userController::changeEmail);
                patch("{id}/password", userController::changePassword);
                delete("{id}", userController::delete);
            });
    }
}
