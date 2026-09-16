package app.task.presentation;

import app.task.presentation.dto.TaskCreateDTO;
import app.task.presentation.dto.TaskDTO;
import app.task.domain.ITaskService;
import app.shared.presentation.CrudController;

public class TaskController extends CrudController<TaskCreateDTO, TaskDTO>
{
    public TaskController(ITaskService taskService)
    {
        super(taskService);
    }
}
