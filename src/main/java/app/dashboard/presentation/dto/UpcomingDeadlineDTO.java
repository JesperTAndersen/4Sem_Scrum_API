package app.dashboard.presentation.dto;

import java.time.LocalDate;

public record UpcomingDeadlineDTO(
        Long projectId,
        String title,
        LocalDate deadline,
        LocalDate calculatedFinishDate,
        Boolean feasible
)
{
}
