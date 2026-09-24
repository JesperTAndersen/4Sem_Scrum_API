package app.task.data;

import java.math.BigDecimal;

import app.competence.domain.Competence;
import app.task.domain.Task;
import app.task.presentation.dto.TaskDTO;

public final class TaskMapper
{
    private TaskMapper() {}

    public static TaskDTO toDTO(Task task)
    {
        Competence competence = task.getCompetence();
        Long competenceId = competence != null ? competence.getId() : 0L;
        return new TaskDTO(
                task.getId(),
                task.getName(),
                task.getMinDuration(),
                competenceId,
                task.getEstimate(),
                task.getCost(),
                task.getStatus()
        );
    }
}