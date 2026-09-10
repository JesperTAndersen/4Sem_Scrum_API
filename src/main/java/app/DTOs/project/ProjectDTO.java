package app.DTOs.project;

import app.entities.ProjectStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectDTO(
        Long id,
        String title,
        String description,
        LocalDate startDate,
        LocalDate deadline,
        ProjectStatus status,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt
) {
}