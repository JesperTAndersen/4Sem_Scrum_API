package app.task.presentation.dto;

import java.util.Set;

public record TaskUpdateDTO(
        String name,
        Long competenceId,
        Double estimate,
        int minimumDurationInDays)
{
}
