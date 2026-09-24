package app.task.presentation.dto;

import java.math.BigDecimal;

import app.competence.presentation.dto.SlimCompetenceDTO;
import app.task.domain.Task.TaskStatus;

public record TaskDTO(
        Long id,
        String name,
        int minimumDurationInDays,
        SlimCompetenceDTO competence,
        double estimate,
        BigDecimal cost,
        double laborDurationInDays,
        double scheduledDurationInDays,
        TaskStatus status)
{
}
