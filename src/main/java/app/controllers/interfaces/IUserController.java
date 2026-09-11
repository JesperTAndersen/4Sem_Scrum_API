package app.controllers.interfaces;

import app.controllers.interfaces.generic.ICrudController;
import io.javalin.http.Context;

public interface IUserController extends ICrudController
{
    void changeRole(Context ctx);

    void changeEmail(Context ctx);

    void changePassword(Context ctx);

    void getMe(Context ctx);
}
