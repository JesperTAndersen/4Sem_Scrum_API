package app.dashboard.presentation;

import java.time.LocalDate;

public record UpcomingDeadlineDTO(
        Long projectId,
        String title,
        LocalDate deadline,
        LocalDate calculatedFinishDate,
        boolean feasible
)
{
}
