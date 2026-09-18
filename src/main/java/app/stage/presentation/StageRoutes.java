package app.stage.presentation;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.put;
import static io.javalin.apibuilder.ApiBuilder.post;

import app.shared.presentation.ICrudController;
import app.security.domain.Role;
import io.javalin.apibuilder.EndpointGroup;

public class StageRoutes
{
    private final ICrudController stageController;

    public StageRoutes(ICrudController stageController)
    {
        this.stageController = stageController;
    }

    public EndpointGroup getRoutes()
    {
        return () ->
            path("stages", () ->
            {
                get(stageController::getAll, Role.PROJECT_MANAGER);
                get("/{id}", stageController::get, Role.PROJECT_MANAGER);
                post(stageController::create, Role.PROJECT_MANAGER);
                put("/{id}", stageController::update, Role.PROJECT_MANAGER);
                delete("/{id}", stageController::delete, Role.PROJECT_MANAGER);
            });
    }
}
