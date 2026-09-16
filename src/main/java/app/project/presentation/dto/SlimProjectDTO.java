package app.project.presentation.dto;

import app.project.domain.ProjectStatus;
import app.task.presentation.dto.TaskCountDTO;
import app.user.presentation.dto.UserReferenceDTO;

import java.time.LocalDate;

public record SlimProjectDTO(
        long id,
        String title,
        String description,
        UserReferenceDTO createdBy,
        LocalDate startDate,
        LocalDate deadline,
        TaskCountDTO taskCountDTO,
        ProjectStatus status
) {
}
