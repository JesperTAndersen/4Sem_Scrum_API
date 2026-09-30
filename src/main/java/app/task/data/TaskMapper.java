package app.task.data;

import java.time.LocalDate;
import java.time.ZoneOffset;

import app.competence.domain.Competence;
import app.competence.presentation.dto.SlimCompetenceDTO;
import app.task.domain.Task;
import app.task.presentation.dto.TaskDTO;

public final class TaskMapper
{
    private TaskMapper() {}

    /* FIXME: change to SlimTaskDTO */
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
                LocalDate.ofEpochDay(0),
                LocalDate.ofEpochDay(0),
                task.getLaborDurationInDays(),
                task.getScheduledDurationInDays(),
                task.getStatus(),
                task.getPredecessors().stream().map(Task::getId).sorted().toList(),
                task.getDependencyStartOffsetInDays(),
                task.getDependencyFinishOffsetInDays()
        );
    }

    public static TaskDTO toDTO(Task task, LocalDate startDate)
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
                startDate.plusDays((int)task.getDependencyStartOffsetInDays()),
                startDate.plusDays((int)task.getDependencyFinishOffsetInDays()),
                task.getLaborDurationInDays(),
                task.getScheduledDurationInDays(),
                task.getStatus(),
                task.getPredecessors().stream().map(Task::getId).sorted().toList(),
                task.getDependencyStartOffsetInDays(),
                task.getDependencyFinishOffsetInDays()
                );
    }
}