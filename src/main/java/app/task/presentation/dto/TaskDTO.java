package app.task.presentation.dto;

import java.math.BigDecimal;

import app.task.domain.Task.TaskStatus;

public record TaskDTO(
        Long id,
        String name,
        Double minDuration,
        Long competenceId,
        Double estimate,
        BigDecimal cost,
        TaskStatus status)
{
}