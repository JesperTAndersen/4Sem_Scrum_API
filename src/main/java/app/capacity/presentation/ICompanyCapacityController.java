package app.capacity.presentation;

import io.javalin.http.Context;

public interface ICompanyCapacityController
{
    void get(Context ctx);

    void update(Context ctx);
}