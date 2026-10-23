package app.task.presentation.dto;

public record TaskCountDTO(
        int totalTaskCount,
        int tasksNotStarted,
        int tasksInProgress,
        int taskDone
)
{
}
