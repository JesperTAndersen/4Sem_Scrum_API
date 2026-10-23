package app.workload.presentation.dto;

public record UnassessedTaskDTO(
        Long taskId,
        String taskName,
        Long projectId,
        String projectTitle,
        String reason
)
{}
