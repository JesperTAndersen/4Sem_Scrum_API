package app.task.presentation.dto;

import app.task.domain.Task.TaskStatus;

public record TaskDTO(
        Long id,
        String name,
        int minimumDurationInDays,
        Long competenceId,
        double estimate,
        double laborDurationInDays,
        double scheduledDurationInDays,
        TaskStatus status)
{
}
