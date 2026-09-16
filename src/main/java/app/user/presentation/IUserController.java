package app.user.presentation;

import app.shared.presentation.ICrudController;
import io.javalin.http.Context;

public interface IUserController extends ICrudController
{
    void changeRole(Context ctx);

    void changeEmail(Context ctx);

    void changePassword(Context ctx);

    void getMe(Context ctx);
}
