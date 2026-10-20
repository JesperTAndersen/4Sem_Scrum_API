package app.stage.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        List<LocalDate> taskStarts = stage.getTasks().stream()
                .map(t -> app.project.domain.WorkingDaySchedule.calculateTaskDates(t, projectStart).startDate())
                .toList();
        return stageStart(taskStarts, projectStart);
    }

    private static LocalDate endDate(Stage stage)
    {
        LocalDate projectStart = stage.getProject().getStartDate();
        List<LocalDate> taskEnds = stage.getTasks().stream()
                .map(t -> app.project.domain.WorkingDaySchedule.calculateTaskDates(t, projectStart).endDate())
                .toList();
        return stageEnd(taskEnds, projectStart);
    }

    private static LocalDate stageStart(List<LocalDate> taskStarts, LocalDate fallback)
    {
        if (taskStarts.isEmpty()) return fallback;
        return taskStarts.stream().filter(Objects::nonNull).min(LocalDate::compareTo).orElse(null);
    }

    private static LocalDate stageEnd(List<LocalDate> taskEnds, LocalDate fallback)
    {
        if (taskEnds.isEmpty()) return fallback;
        if (taskEnds.stream().anyMatch(Objects::isNull)) return null;
        return taskEnds.stream().max(LocalDate::compareTo).orElse(fallback);
    }

    public static StageDTO toDTO(Stage stage, LocalDate startDate)
    {
        Set<Task> tasks = stage.getTasks();
        List<TaskDTO> taskDTOs = new ArrayList<>(tasks.size());
        List<LocalDate> taskStarts = new ArrayList<>(tasks.size());
        List<LocalDate> taskEnds = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            TaskDTO dto = TaskMapper.toDTO(task, startDate);
            taskDTOs.add(dto);
            taskStarts.add(dto.startDate());
            taskEnds.add(dto.endDate());
        }

        return new StageDTO(
                stage.getId(),
                stage.getName(),
                stage.getTotalEstimatedHours(),
                stage.getTotalCost(),
                stageStart(taskStarts, startDate),
                stageEnd(taskEnds, startDate),
                taskDTOs,
                stage.isCompleted(),
                stage.getPredecessors().stream().map(Stage::getId).sorted().toList()
        );
    }
}
