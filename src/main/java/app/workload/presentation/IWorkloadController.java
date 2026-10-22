package app.workload.presentation;

import io.javalin.http.Context;

public interface IWorkloadController
{
    void getConflicts(Context ctx);
}
