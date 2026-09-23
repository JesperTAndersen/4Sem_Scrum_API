package app.project.presentation.dto;

import app.project.domain.ProjectStatus;
import app.task.presentation.dto.TaskCountDTO;
import app.user.presentation.dto.UserReferenceDTO;
import app.stage.presentation.dto.StageDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
        LocalDateTime updatedAt,
        double totalEstimatedHours,
        List<StageDTO> stages,
        TaskCountDTO tasks
) {
}
