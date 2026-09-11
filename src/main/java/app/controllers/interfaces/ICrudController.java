package app.controllers.interfaces;

import io.javalin.http.Context;

public interface ICrudController
{
    void get(Context ctx);

    void getAll(Context ctx);

    void create(Context ctx);

    void update(Context ctx);

    void delete(Context ctx);
}
