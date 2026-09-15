package app.routes;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.put;
import static io.javalin.apibuilder.ApiBuilder.post;

import app.controllers.interfaces.generic.ICrudController;
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
                get("", stageController::getAll);
                get("{id}", stageController::get);
                post("", stageController::create);
                put("{id}", stageController::update);
                delete("{id}", stageController::delete);
            });
    }
}
