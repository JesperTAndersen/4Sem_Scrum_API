package app.project.presentation.dto;

import app.project.domain.ProjectStatus;
import app.user.presentation.dto.UserReferenceDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectDTO(
        Long id,
        String title,
        String description,
        LocalDate startDate,
        LocalDate deadline,
        ProjectStatus status,
        UserReferenceDTO createdBy,
        LocalDateTime createdAt,
        UserReferenceDTO updatedBy,
        LocalDateTime updatedAt
) {
}
