package app.task.presentation.dto;

import app.task.domain.Task.TaskStatus;

public record TaskDTO(
        Long id,
        String name,
        float estimate,
        float duration,
        int crewSize,
        TaskStatus status)
{
}
