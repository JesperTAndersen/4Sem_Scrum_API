package app.task.presentation.dto;

import java.math.BigDecimal;
import java.util.List;

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
        TaskStatus status,
        List<Long> predecessorIds // TODO added in feat/task-dependencies
)
{
}