package app.task.presentation.dto;

import app.task.domain.Task.TaskStatus;

import java.time.LocalDate;
import java.util.List;

public record SlimTaskDTO(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        TaskStatus status,
        List<Long> predecessorIds,
        double dependencyStartOffsetInDays,
        double dependencyFinishOffsetInDays
)
{
}
