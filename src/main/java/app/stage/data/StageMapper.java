package app.stage.data;

import app.stage.domain.Stage;
import app.stage.presentation.dto.StageDTO;
import app.task.data.TaskMapper;

public final class StageMapper
{
    private StageMapper() {}

    public static StageDTO toDTO(Stage stage)
    {
        return new StageDTO(
                stage.getId(),
                stage.getName(),
                stage.getTotalEstimatedHours(),
                stage.getTasks().stream().map(TaskMapper::toDTO).toList()
        );
    }
}
