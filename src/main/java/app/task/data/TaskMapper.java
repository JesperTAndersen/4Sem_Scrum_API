package app.task.data;

import java.time.LocalDate;

import app.competence.domain.Competence;
import app.competence.presentation.dto.SlimCompetenceDTO;
import app.project.domain.WorkingDaySchedule;
import app.task.domain.Task;
import app.task.presentation.dto.TaskDTO;

public final class TaskMapper
{
    private TaskMapper() {}

    public static TaskDTO toDTO(Task task)
    {
        return toDTO(task, projectStartDate(task));
    }

    public static TaskDTO toDTO(Task task, LocalDate projectStartDate)
    {
        Competence competence = task.getCompetence();
        SlimCompetenceDTO competenceDTO = competence == null
                ? null
                : new SlimCompetenceDTO(competence.getId(), competence.getName());
        WorkingDaySchedule.TaskDates dates = WorkingDaySchedule.calculateTaskDates(task, projectStartDate);
        return new TaskDTO(
                task.getId(),
                task.getName(),
                task.getMinimumDurationInDays(),
                competenceDTO,
                task.getEstimate(),
                task.getCost(),
                dates.startDate(),
                dates.endDate(),
                task.getLaborDurationInDays(),
                task.getScheduledDurationInDays(),
                task.getStatus(),
                task.getPredecessors().stream().map(Task::getId).sorted().toList(),
                task.getDependencyStartOffsetInDays(),
                task.getDependencyFinishOffsetInDays()
        );
    }

    private static LocalDate projectStartDate(Task task)
    {
        return task.getStage() == null || task.getStage().getProject() == null
                ? LocalDate.ofEpochDay(0)
                : task.getStage().getProject().getStartDate();
    }

}
