package app.dtos.project;

import app.enums.ProjectStatus;

import java.time.LocalDate;

public record UpdateProjectDTO(
        String title,
        String description,
        LocalDate startDate,
        LocalDate deadline,
        ProjectStatus status
)
{
}
