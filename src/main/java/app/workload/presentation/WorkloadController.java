package app.workload.presentation;

import app.workload.domain.IEmployeeWorkloadService;
import app.utils.RequestUtil;
import app.exceptions.BadRequestException;
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
    public void getAvailability(Context ctx)
    {
        LocalDate date = RequestUtil.requireQueryDate(ctx, "date");
        Long competenceId = null;
        String competenceParam = ctx.queryParam("competenceId");
        if (competenceParam != null)
        {
            try
            {
                competenceId = Long.valueOf(competenceParam);
            }
            catch (NumberFormatException e)
            {
                throw new BadRequestException("Competence id must be a positive integer");
            }
        }
        ctx.status(HttpStatus.OK).json(employeeWorkloadService.findAvailability(date, competenceId));
    }

    @Override
    public void getConflicts(Context ctx)
    {
        LocalDate startDate = RequestUtil.requireQueryDate(ctx, "startDate");
        LocalDate endDate = RequestUtil.requireQueryDate(ctx, "endDate");
        ctx.status(HttpStatus.OK).json(employeeWorkloadService.findConflicts(startDate, endDate));
    }
}
