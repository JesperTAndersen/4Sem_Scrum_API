package app.dashboard.presentation;

import io.javalin.http.Context;

public interface IDashboardController
{
    void getSummary(Context ctx);
}
