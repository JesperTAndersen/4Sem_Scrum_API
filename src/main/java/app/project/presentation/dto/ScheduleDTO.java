package app.project.presentation.dto;

import java.time.LocalDate;

public record ScheduleDTO(
        LocalDate calculatedFinishDate,
        Boolean feasible
)
{
}
