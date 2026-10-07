package app.capacity.presentation;

import app.security.domain.Role;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.put;

public class CompanyCapacityRoutes
{
    private final ICompanyCapacityController companyCapacityController;

    public CompanyCapacityRoutes(ICompanyCapacityController companyCapacityController)
    {
        this.companyCapacityController = companyCapacityController;
    }

    public EndpointGroup getRoutes()
    {
        return () -> path("company-capacities", () ->
        {
            get(companyCapacityController::get, Role.PROJECT_MANAGER);
            put(companyCapacityController::update, Role.PROJECT_MANAGER);
        });
    }
}