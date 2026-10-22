package app.workload.presentation;

import app.workload.domain.IEmployeeWorkloadService;
import app.utils.RequestUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;

public class WorkloadController implements IWorkloadController
{
    private final IEmployeeWorkloadService employeeWorkloadService;

    public WorkloadController(IEmployeeWorkloadService employeeWorkloadService)
    {
        this.employeeWorkloadService = employeeWorkloadService;
    }

    @Override
    public void getConflicts(Context ctx)
    {
        LocalDate startDate = RequestUtil.requireQueryDate(ctx, "startDate");
        LocalDate endDate = RequestUtil.requireQueryDate(ctx, "endDate");
        ctx.status(HttpStatus.OK).json(employeeWorkloadService.findConflicts(startDate, endDate));
    }
}
