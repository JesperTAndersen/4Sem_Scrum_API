package app.task.data;

import app.competence.data.CompetenceMapper;
import app.competence.presentation.dto.CompetenceDTO;
import app.task.domain.Task;
import app.task.presentation.dto.TaskDTO;

import java.util.Comparator;

public final class TaskMapper
{
    private TaskMapper() {}

    public static TaskDTO toDTO(Task task)
    {
        return new TaskDTO(
                task.getId(),
                task.getName(),
                task.getEstimate(),
                task.getMinimumDurationInDays(),
                task.getLaborDurationInDays(),
                task.getScheduledDurationInDays(),
                task.getStatus(),
                task.getRequiredCompetences().stream()
                        .map(CompetenceMapper::toDTO)
                        .sorted(Comparator.comparing(CompetenceDTO::id))
                        .toList()
        );
    }
}
