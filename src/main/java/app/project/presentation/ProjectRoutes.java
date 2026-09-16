package app.project.presentation;

import app.shared.presentation.ICrudController;
import app.security.domain.Role;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class ProjectRoutes
{
    private final ICrudController projectController;

    public ProjectRoutes(ICrudController projectController)
    {
        this.projectController = projectController;
    }

    public EndpointGroup getRoutes()
    {
        return () -> path("projects", () ->
        {
            get(projectController::getAll, Role.PROJECT_MANAGER);
            get("/{id}", projectController::get, Role.PROJECT_MANAGER);
            post(projectController::create, Role.PROJECT_MANAGER);
            put("/{id}", projectController::update, Role.PROJECT_MANAGER);
            delete("/{id}", projectController::delete, Role.PROJECT_MANAGER);
        });
    }
}
