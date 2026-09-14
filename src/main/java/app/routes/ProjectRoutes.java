package app.routes;

import app.controllers.interfaces.generic.ICrudController;
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
            get(projectController::getAll);
            get("/{id}", projectController::get);
            post(projectController::create);
            put("/{id}", projectController::update);
            delete("/{id}", projectController::delete);
        });
    }
}
