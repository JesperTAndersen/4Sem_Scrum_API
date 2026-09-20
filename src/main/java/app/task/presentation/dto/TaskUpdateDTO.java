package app.task.presentation.dto;

import java.util.Set;

public record TaskUpdateDTO(
        String name,
        Double estimate,
        Set<Long> competenceIds)
{
}
