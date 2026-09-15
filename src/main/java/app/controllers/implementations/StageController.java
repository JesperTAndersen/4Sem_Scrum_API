package app.controllers.implementations;

import app.dtos.stage.StageCreateDTO;
import app.dtos.stage.StageDTO;
import app.services.interfaces.IStageService;

public class StageController extends CrudController<StageCreateDTO, StageDTO>
{
    public StageController(IStageService stageService)
    {
        super(stageService);
    }
}
