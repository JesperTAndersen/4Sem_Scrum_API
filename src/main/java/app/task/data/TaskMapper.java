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
        Long competenceId = 0L;
        BigDecimal cost = new BigDecimal(0.0);
        Competence competence = task.getCompetence();
        if (competence != null) {
            competenceId = competence.getId();
            cost = competence.getRate().multiply(new BigDecimal(task.getEstimate()));
        }
        return new TaskDTO(
                task.getId(),
                task.getName(),
                task.getMinDuration(),
                competenceId,
                task.getEstimate(),
                cost,
                task.getStatus()
        );
    }
}