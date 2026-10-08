package app.user.presentation;

import app.shared.presentation.ICrudController;
import io.javalin.http.Context;

public interface IUserController
{
    void get(Context ctx);

    void getAll(Context ctx);

    void update(Context ctx);

    void delete(Context ctx);

    void changeRole(Context ctx);

    void changeEmail(Context ctx);

    void changePassword(Context ctx);

    void getMe(Context ctx);
}