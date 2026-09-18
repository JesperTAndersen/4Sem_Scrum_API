package app.stage.domain;

import app.stage.presentation.dto.StageCreateDTO;
import app.stage.presentation.dto.StageDTO;
import app.stage.presentation.dto.StageUpdateDTO;
import app.shared.domain.ICrudService;

public interface IStageService extends ICrudService<StageCreateDTO, StageDTO>
{
    StageDTO update(Long id, StageUpdateDTO dto);
}
