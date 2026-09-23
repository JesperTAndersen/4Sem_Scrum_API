package app.task.domain;

import java.util.List;

import app.shared.domain.ICrudService;
import app.task.presentation.dto.TaskCreateDTO;
import app.task.presentation.dto.TaskDTO;
import app.task.presentation.dto.TaskUpdateDTO;

public interface ITaskService extends ICrudService<TaskCreateDTO, TaskDTO>
{
    TaskDTO update(Long id, TaskUpdateDTO dto);
}