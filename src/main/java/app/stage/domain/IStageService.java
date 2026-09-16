package app.stage.domain;

import app.stage.presentation.dto.StageCreateDTO;
import app.stage.presentation.dto.StageDTO;
import app.shared.domain.ICrudService;

public interface IStageService extends ICrudService<StageCreateDTO, StageDTO>
{
}
