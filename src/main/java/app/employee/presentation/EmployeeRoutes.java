package app.employee.presentation;

import app.security.domain.Role;
import app.workload.presentation.IWorkloadController;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class EmployeeRoutes
{
    private final IEmployeeController employeeController;

    private final IWorkloadController workloadController;

    public EmployeeRoutes(IEmployeeController employeeController, IWorkloadController workloadController)
    {
        this.employeeController = employeeController;
        this.workloadController = workloadController;
    }

    public EndpointGroup getRoutes()
    {
        return () -> path("employee", () ->
        {
            get(employeeController::getAll, Role.PROJECT_MANAGER);
            get("/capacity", employeeController::getCapacity, Role.PROJECT_MANAGER);
            get("/over-allocations", workloadController::getConflicts, Role.PROJECT_MANAGER);
            get("/{id}", employeeController::get, Role.PROJECT_MANAGER);
            post(employeeController::create, Role.PROJECT_MANAGER);
            put("/{id}", employeeController::update, Role.PROJECT_MANAGER);
            put("/{id}/competences", employeeController::updateCompetences, Role.PROJECT_MANAGER);
            patch("/{id}/activate", employeeController::setActive, Role.PROJECT_MANAGER);
            patch("/{id}/deactivate", employeeController::setInactive, Role.PROJECT_MANAGER);
            delete("/{id}", employeeController::delete, Role.PROJECT_MANAGER);
        });
    }
}