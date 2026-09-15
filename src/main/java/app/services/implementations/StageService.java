package app.services.implementations;

import app.dtos.stage.StageCreateDTO;
import app.dtos.stage.StageDTO;
import app.entities.Project;
import app.entities.Stage;
import app.persistence.interfaces.specific.IProjectDAO;
import app.persistence.interfaces.specific.IStageDAO;
import app.services.interfaces.IStageService;
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
        stage.name = dto.name().trim();
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
        return new StageDTO(stage.id(), stage.name);
    }
}
