package app.DTOs.project;

import app.entities.ProjectStatus;

import java.time.LocalDate;

public record CreateProjectDTO(
        String title,
        String description,
        LocalDate startDate,
        LocalDate deadline,
        ProjectStatus status
) {
}