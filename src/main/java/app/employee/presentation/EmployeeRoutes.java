package app.employee.presentation;

import app.competence.presentation.ICompetenceController;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class EmployeeRoutes
{
    private final IEmployeeController employeeController;

    public EmployeeRoutes(IEmployeeController employeeController)
    {
        this.employeeController = employeeController;
    }

    public EndpointGroup getRoutes()
    {
        return () -> path("employee", () ->
        {
            get(employeeController::getAll);
            get("/{id}", employeeController::get);
            post(employeeController::create);
            put("/{id}", employeeController::update);
            patch("/{id}/activate", employeeController::setActive);
            patch("/{id}/deactivate", employeeController::setInactive);
            delete("/{id}", employeeController::delete);
        });
    }
}
