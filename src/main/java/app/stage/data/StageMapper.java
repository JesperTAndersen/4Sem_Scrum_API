package app.stage.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    /* FIXME: change to SlimStageDTO */
    public static StageDTO toDTO(Stage stage)
    {
        return new StageDTO(
                stage.getId(),
                stage.getName(),
                stage.getTotalEstimatedHours(),
                stage.getTotalCost(),
                LocalDate.ofEpochDay(0),
                LocalDate.ofEpochDay(0),
                stage.getTasks().stream().map(TaskMapper::toDTO).toList()
        );
    }

    public static StageDTO toDTO(Stage stage, LocalDate startDate)
    {
        LocalDate endDate = startDate;
        Set<Task> tasks = stage.getTasks();
        List<TaskDTO> taskDTOs = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            TaskDTO dto = TaskMapper.toDTO(task, startDate);
            taskDTOs.add(dto);
            if (dto.endDate().isAfter(endDate))
                    endDate = dto.endDate();
        }

        return new StageDTO(
                stage.getId(),
                stage.getName(),
                stage.getTotalEstimatedHours(),
                stage.getTotalCost(),
                startDate,
                endDate,
                taskDTOs
        );
    }
}