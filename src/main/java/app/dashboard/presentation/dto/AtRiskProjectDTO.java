package app.dashboard.presentation.dto;

import java.time.LocalDate;

public record AtRiskProjectDTO(
        Long projectId,
        String title,
        LocalDate deadline,
        LocalDate calculatedFinishDate
)
{
}
