package app.task.data;

import app.task.domain.Task;
import app.task.presentation.dto.TaskDTO;

public final class TaskMapper
{
    private TaskMapper() {}

    public static TaskDTO toDTO(Task task)
    {
        return new TaskDTO(
                task.getId(),
                task.getName(),
                task.getEstimate(),
                task.getDuration(),
                task.getCrewSize(),
                task.getStatus()
        );
    }
}
