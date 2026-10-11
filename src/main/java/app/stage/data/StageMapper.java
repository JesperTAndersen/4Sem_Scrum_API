package app.stage.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import app.stage.domain.Stage;
import app.stage.presentation.dto.StageDTO;
import app.task.data.TaskMapper;
import app.task.domain.Task;
import app.task.presentation.dto.TaskDTO;

public final class StageMapper
{
    private StageMapper() {}

    public static StageDTO toDTO(Stage stage)
    {
        return new StageDTO(
                stage.getId(),
                stage.getName(),
                stage.getTotalEstimatedHours(),
                stage.getTotalCost(),
                startDate(stage),
                endDate(stage),
                stage.getTasks().stream().map(TaskMapper::toDTO).toList(),
                stage.isCompleted(),
                stage.getPredecessors().stream().map(Stage::getId).sorted().toList()
        );
    }

    private static LocalDate startDate(Stage stage)
    {
        LocalDate projectStart = stage.getProject().getStartDate();
        return stage.getTasks().stream().map(t -> app.project.domain.WorkingDaySchedule.calculateTaskDates(t, projectStart).startDate())
                .min(LocalDate::compareTo).orElse(projectStart);
    }

    private static LocalDate endDate(Stage stage)
    {
        LocalDate projectStart = stage.getProject().getStartDate();
        return stage.getTasks().stream().map(t -> app.project.domain.WorkingDaySchedule.calculateTaskDates(t, projectStart).endDate())
                .max(LocalDate::compareTo).orElse(projectStart);
    }

    public static StageDTO toDTO(Stage stage, LocalDate startDate)
    {
        LocalDate endDate = startDate;
        LocalDate earliest = null;
        Set<Task> tasks = stage.getTasks();
        List<TaskDTO> taskDTOs = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            TaskDTO dto = TaskMapper.toDTO(task, startDate);
            taskDTOs.add(dto);
            if (earliest == null || dto.startDate().isBefore(earliest)) earliest = dto.startDate();
            if (dto.endDate().isAfter(endDate))
                    endDate = dto.endDate();
        }

        return new StageDTO(
                stage.getId(),
                stage.getName(),
                stage.getTotalEstimatedHours(),
                stage.getTotalCost(),
                earliest == null ? startDate : earliest,
                endDate,
                taskDTOs,
                stage.isCompleted(),
                stage.getPredecessors().stream().map(Stage::getId).sorted().toList()
        );
    }
}
