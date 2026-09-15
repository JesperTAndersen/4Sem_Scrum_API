package app.services.interfaces;

import app.dtos.task.TaskCreateDTO;
import app.dtos.task.TaskDTO;

public interface ITaskService extends ICrudService<TaskCreateDTO, TaskDTO>
{
}
