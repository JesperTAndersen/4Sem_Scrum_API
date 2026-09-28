package app.task.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import app.task.domain.Task.TaskStatus;

public record TaskDTO(
        Long id,
        String name,
        int minimumDurationInDays,
        Long competenceId,
        double estimate,
        BigDecimal cost,
        LocalDate startDate,
        LocalDate endDate,
        double laborDurationInDays,
        double scheduledDurationInDays,
        TaskStatus status)
{
}