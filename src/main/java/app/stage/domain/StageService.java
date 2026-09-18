package app.stage.domain;

import app.stage.presentation.dto.StageCreateDTO;
import app.stage.presentation.dto.StageDTO;
import app.project.domain.Project;
import app.stage.domain.Stage;
import app.project.data.IProjectDAO;
import app.stage.data.IStageDAO;
import app.stage.domain.IStageService;
import app.utils.ValidationUtil;

import java.util.List;

public class StageService implements IStageService
{
    private final IStageDAO stageDAO;
    private final IProjectDAO projectDAO;

    public StageService(IStageDAO stageDAO, IProjectDAO projectDAO)
    {
        this.stageDAO = stageDAO;
        this.projectDAO = projectDAO;
    }

    @Override
    public StageDTO create(StageCreateDTO dto)
    {
        ValidationUtil.validateNotNull(dto, "Stage");
        ValidationUtil.validateId(dto.projectId());
        validateName(dto.name());

        Project project = projectDAO.get(dto.projectId());
        Stage created = stageDAO.create(new Stage(dto.name().trim(), project));
        return toDTO(created);
    }

    @Override
    public StageDTO get(Long id)
    {
        return toDTO(stageDAO.get(id));
    }

    @Override
    public List<StageDTO> getAll()
    {
        return stageDAO.getAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public StageDTO update(StageDTO dto)
    {
        ValidationUtil.validateNotNull(dto, "Stage");
        ValidationUtil.validateId(dto.id());
        validateName(dto.name());

        Stage stage = stageDAO.get(dto.id());
        stage.update(dto.name().trim());
        stageDAO.update(stage);
        return toDTO(stageDAO.update(stage));
    }

    @Override
    public void delete(Long id)
    {
        stageDAO.delete(id);
    }

    private void validateName(String name)
    {
        ValidationUtil.validateNotBlank(name, "Stage name");
    }

    private StageDTO toDTO(Stage stage)
    {
        return new StageDTO(stage.getId(), stage.getName(), List.of());
    }
}
