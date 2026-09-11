package app.controllers;

import io.javalin.http.Context;

public interface ICrudController
{
    void getById(Context ctx);

    void getAll(Context ctx);

    void create(Context ctx);

    void update(Context ctx);

    void delete(Context ctx);
}
