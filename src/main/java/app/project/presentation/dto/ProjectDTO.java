package app.project.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import app.project.domain.ProjectStatus;
import app.stage.presentation.dto.StageDTO;
import app.task.presentation.dto.TaskCountDTO;
import app.user.presentation.dto.UserReferenceDTO;

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
        BigDecimal totalCost,
        List<StageDTO> stages,
        TaskCountDTO tasks,
        ScheduleDTO schedule
) {
}