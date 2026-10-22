package app.workload.presentation.dto;

public record TaskHoursDTO(
        Long projectId,
        String projectTitle,
        Long taskId,
        String taskName,
        double allocatedHours
)
{}