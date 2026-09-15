package app.dtos.project;

import java.time.LocalDate;

public record CreateProjectDTO(
        String title,
        String description,
        LocalDate startDate,
        LocalDate deadline
) {
}
