package app.task.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import app.competence.presentation.dto.SlimCompetenceDTO;
import app.task.domain.Task.TaskStatus;

public record TaskDTO(
        Long id,
        String name,
        int minimumDurationInDays,
        SlimCompetenceDTO competence,
        double estimate,
        BigDecimal cost,
        LocalDate startDate,
        LocalDate endDate,
        double laborDurationInDays,
        double scheduledDurationInDays,
        TaskStatus status,
        List<Long> predecessorIds,
        double dependencyStartOffsetInDays,
        double dependencyFinishOffsetInDays
)
{
}