package app.task.presentation;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

import app.security.domain.Role;
import io.javalin.apibuilder.EndpointGroup;

public class TaskRoutes
{
    private final TaskController taskController;

    public TaskRoutes(TaskController taskController)
    {
        this.taskController = taskController;
    }

    public EndpointGroup getRoutes()
    {
        return () ->
            path("tasks", () ->
            {
                get(taskController::getAll, Role.PROJECT_MANAGER);
                get("/{id}", taskController::get, Role.PROJECT_MANAGER);
                post(taskController::create, Role.PROJECT_MANAGER);
                put("/{id}", taskController::update, Role.PROJECT_MANAGER);
                delete("/{id}", taskController::delete, Role.PROJECT_MANAGER);
            });
    }
}