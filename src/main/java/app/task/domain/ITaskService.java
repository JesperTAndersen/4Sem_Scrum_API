package app.task.domain;

import app.task.presentation.dto.TaskCreateDTO;
import app.task.presentation.dto.TaskDTO;
import app.task.presentation.dto.TaskUpdateDTO;
import app.shared.domain.ICrudService;

public interface ITaskService extends ICrudService<TaskCreateDTO, TaskDTO>
{
    TaskDTO update(Long id, TaskUpdateDTO dto);
}
