package app.workload.presentation;

import io.javalin.http.Context;

public interface IWorkloadController
{
    void getAvailability(Context ctx);

    void getConflicts(Context ctx);
}
