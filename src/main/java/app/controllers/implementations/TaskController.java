package app.controllers.implementations;

import app.dtos.task.TaskCreateDTO;
import app.dtos.task.TaskDTO;
import app.services.interfaces.ITaskService;

public class TaskController extends CrudController<TaskCreateDTO, TaskDTO>
{
    public TaskController(ITaskService taskService)
    {
        super(taskService);
    }
}
