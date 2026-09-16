package app.project.presentation.dto;

import app.project.domain.ProjectStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectDTO(
        Long id,
        String title,
        String description,
        LocalDate startDate,
        LocalDate deadline,
        ProjectStatus status,
        String createdBy, //TODO UserReferenceDTO
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt
) {
}
