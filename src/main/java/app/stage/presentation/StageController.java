package app.stage.presentation;

import app.stage.presentation.dto.StageCreateDTO;
import app.stage.presentation.dto.StageDTO;
import app.stage.domain.IStageService;
import app.shared.presentation.CrudController;

public class StageController extends CrudController<StageCreateDTO, StageDTO>
{
    public StageController(IStageService stageService)
    {
        super(stageService);
    }
}
