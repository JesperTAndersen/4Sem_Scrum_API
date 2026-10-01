package app.employee.presentation;

import app.employee.domain.IEmployeeService;
import app.employee.presentation.dto.EmployeeCreateDTO;
import app.employee.presentation.dto.EmployeeUpdateDTO;
import app.utils.RequestUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Objects;

public class EmployeeController implements IEmployeeController
{
    private final IEmployeeService employeeService;

    public EmployeeController(IEmployeeService employeeService)
    {
        this.employeeService = employeeService;
    }

    @Override
    public void create(Context ctx)
    {
        EmployeeCreateDTO body = ctx.bodyValidator(EmployeeCreateDTO.class)
                .check(Objects::nonNull, "Employee payload cannot be null")
                .check(employee -> employee != null && employee.firstName() != null && !employee.firstName().isBlank(), "First name is required")
                .check(employee -> employee != null && employee.lastName() != null && !employee.lastName().isBlank(), "Last name is required")
                .check(employee -> employee != null && employee.dailyCapacity() != null
                        && Double.isFinite(employee.dailyCapacity()) && employee.dailyCapacity() > 0,
                        "Daily capacity must be a positive number")
                .get();

        ctx.status(HttpStatus.CREATED).json(employeeService.create(body));
    }

    @Override
    public void get(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        ctx.json(employeeService.get(id));
    }

    @Override
    public void getAll(Context ctx)
    {
        ctx.json(employeeService.getAll());
    }

    @Override
    public void update(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        EmployeeUpdateDTO body = ctx.bodyValidator(EmployeeUpdateDTO.class)
                .check(Objects::nonNull, "Employee payload cannot be null")
                .check(employee -> employee != null && employee.firstName() != null && !employee.firstName().isBlank(), "First name is required")
                .check(employee -> employee != null && employee.lastName() != null && !employee.lastName().isBlank(), "Last name is required")
                .check(employee -> employee != null && employee.dailyCapacity() != null
                        && Double.isFinite(employee.dailyCapacity()) && employee.dailyCapacity() > 0,
                        "Daily capacity must be a positive number")
                .get();

        ctx.json(employeeService.update(id, body));
    }

    @Override
    public void delete(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        employeeService.delete(id);
        ctx.status(HttpStatus.NO_CONTENT);
    }

    @Override
    public void setActive(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        employeeService.setActive(id, true);
        ctx.status(HttpStatus.NO_CONTENT);
    }

    @Override
    public void setInactive(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        employeeService.setActive(id, false);
        ctx.status(HttpStatus.NO_CONTENT);
    }
}
