package app.DTOs.project;

import app.entities.ProjectStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UpdateProjectDTO(
        Long id,
        String title,
        String description,
        LocalDate startDate,
        LocalDate deadline,
        ProjectStatus status,
        String updatedBy,
        LocalDateTime updatedAt
)
{
}
