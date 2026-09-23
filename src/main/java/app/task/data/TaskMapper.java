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
                task.getMinimumDurationInDays(),
                task.getLaborDurationInDays(),
                task.getScheduledDurationInDays(),
                task.getStatus(),
                task.getRequiredCompetences().stream()
                        .map(CompetenceMapper::toDTO)
                        .sorted(Comparator.comparing(CompetenceDTO::id))
                        .toList()
                task.getStatus()
        );
    }
}