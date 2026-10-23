package app.capacity.presentation;

import app.capacity.domain.ICompanyCapacityService;
import app.capacity.presentation.dto.UpdateCompanyCapacityDTO;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Objects;

public class CompanyCapacityController implements ICompanyCapacityController
{
    private final ICompanyCapacityService companyCapacityService;

    public CompanyCapacityController(ICompanyCapacityService companyCapacityService)
    {
        this.companyCapacityService = companyCapacityService;
    }

    @Override
    public void get(Context ctx)
    {
        ctx.status(HttpStatus.OK).json(companyCapacityService.get());
    }

    @Override
    public void update(Context ctx)
    {
        UpdateCompanyCapacityDTO dto = ctx.bodyValidator(UpdateCompanyCapacityDTO.class)
                .check(Objects::nonNull, "Company capacity payload is required")
                .check(capacity -> capacity.dailyCapacity() != null, "Daily capacity is required")
                .get();
        ctx.status(HttpStatus.OK).json(companyCapacityService.update(dto));
    }
}