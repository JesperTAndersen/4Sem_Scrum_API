package app.stage.presentation.dto;

import app.task.presentation.dto.TaskDTO;
import java.util.List;

public record StageDTO(
        Long id,
        String name,
        List<TaskDTO> tasks)
{
}
