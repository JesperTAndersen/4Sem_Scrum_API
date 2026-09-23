package app.task.data;

import app.task.domain.Task;
import app.task.presentation.dto.TaskDTO;

public final class TaskMapper
{
    private TaskMapper() {}

    public static TaskDTO toDTO(Task task)
    {
        Long competenceId = task.getCompetence() != null ? task.getCompetence().getId() : 0L;
        return new TaskDTO(
                task.getId(),
                task.getName(),
                task.getMinDuration(),
                competenceId,
                task.getEstimate(),
                task.getStatus()
        );
    }
}