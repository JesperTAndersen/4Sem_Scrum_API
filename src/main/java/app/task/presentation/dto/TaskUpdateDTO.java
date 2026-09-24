package app.task.presentation.dto;

import app.task.domain.Task.TaskStatus;

public record TaskUpdateDTO(
        String name,
        Integer minimumDurationInDays,
        Long competenceId,
        Double estimate,
        TaskStatus status)
{
}
