package app.project.presentation.dto;

import app.project.domain.ProjectStatus;

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
