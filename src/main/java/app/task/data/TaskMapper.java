package app.task.data;

import java.time.LocalDate;
import java.time.ZoneOffset;

import app.competence.domain.Competence;
import app.task.domain.Task;
import app.task.presentation.dto.TaskDTO;

public final class TaskMapper
{
    private TaskMapper() {}

    /* FIXME: change to SlimTaskDTO */
    public static TaskDTO toDTO(Task task)
    {
        Competence competence = task.getCompetence();
        Long competenceId = competence != null ? competence.getId() : 0L;
        return new TaskDTO(
                task.getId(),
                task.getName(),
                task.getMinimumDurationInDays(),
                competenceId,
                task.getEstimate(),
                task.getCost(),
                LocalDate.ofEpochDay(0),
                LocalDate.ofEpochDay(0),
                task.getLaborDurationInDays(),
                task.getScheduledDurationInDays(),
                task.getStatus());
    }
    public static TaskDTO toDTO(Task task, LocalDate startDate)
    {
        Competence competence = task.getCompetence();
        Long competenceId = competence != null ? competence.getId() : 0L;
        double schedDays = task.getScheduledDurationInDays();
        return new TaskDTO(
                task.getId(),
                task.getName(),
                task.getMinimumDurationInDays(),
                competenceId,
                task.getEstimate(),
                task.getCost(),
                startDate,
                startDate.plusDays((long)schedDays),
                task.getLaborDurationInDays(),
                schedDays,
                task.getStatus());
    }
}