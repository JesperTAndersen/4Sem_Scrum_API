package app.task.data;

import app.competence.domain.Competence;
import app.competence.presentation.dto.SlimCompetenceDTO;
import app.task.domain.Task;
import app.task.presentation.dto.TaskDTO;

public final class TaskMapper
{
    private TaskMapper() {}

    public static TaskDTO toDTO(Task task)
    {
        Competence competence = task.getCompetence();
        SlimCompetenceDTO competenceDTO = competence == null
                ? null
                : new SlimCompetenceDTO(competence.getId(), competence.getName());
        return new TaskDTO(
                task.getId(),
                task.getName(),
                task.getMinimumDurationInDays(),
                competenceDTO,
                task.getEstimate(),
                task.getCost(),
                task.getLaborDurationInDays(),
                task.getScheduledDurationInDays(),
                task.getStatus());
    }
}
