package app.task.presentation.dto;

import app.task.domain.Task.TaskStatus;

public record TaskDTO(
        Long id,
        String name,
        Double minDuration,
        Long competenceId,
        Double estimate,
        TaskStatus status)
{
}