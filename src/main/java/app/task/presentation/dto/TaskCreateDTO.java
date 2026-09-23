package app.task.presentation.dto;

import java.util.Set;

public record TaskCreateDTO(
        Long stageId,
        String name,
        double estimate,
        Integer minimumDurationInDays,
        Set<Long> competenceIds)
{
}
