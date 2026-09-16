package app.task.presentation;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.put;
import static io.javalin.apibuilder.ApiBuilder.post;

import app.shared.presentation.ICrudController;
import io.javalin.apibuilder.EndpointGroup;

public class TaskRoutes
{
    private final ICrudController taskController;

    public TaskRoutes(ICrudController taskController)
    {
        this.taskController = taskController;
    }

    public EndpointGroup getRoutes()
    {
        return () ->
            path("tasks", () ->
            {
                get("", taskController::getAll);
                get("{id}", taskController::get);
                post("", taskController::create);
                put("{id}", taskController::update);
                delete("{id}", taskController::delete);
            });
    }
}
