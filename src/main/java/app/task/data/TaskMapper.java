package app.task.data;

import app.task.domain.Task;
import app.task.domain.TaskCompetence;
import app.task.presentation.dto.TaskCompetenceDTO;
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
                task.getStatus()
        );
    }

    public static TaskCompetenceDTO toDTO(TaskCompetence task)
    {
        return new TaskCompetenceDTO(
                task.getId(),
                task.getEstimate()
        );
    }
}
