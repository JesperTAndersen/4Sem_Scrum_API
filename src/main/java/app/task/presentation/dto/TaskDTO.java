package app.task.presentation.dto;

import java.math.BigDecimal;

import app.task.domain.Task.TaskStatus;

public record TaskDTO(
        Long id,
        String name,
        int minimumDurationInDays,
        Long competenceId,
        double estimate,
        BigDecimal cost,
        double laborDurationInDays,
        double scheduledDurationInDays,
        TaskStatus status)
{
}