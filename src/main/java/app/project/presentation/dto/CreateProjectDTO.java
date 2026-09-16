package app.project.presentation.dto;

import java.time.LocalDate;

public record CreateProjectDTO(
        String title,
        String description,
        LocalDate startDate,
        LocalDate deadline
) {
}
