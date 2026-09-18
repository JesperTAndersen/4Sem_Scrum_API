package app.stage.domain;

import app.stage.presentation.dto.StageCreateDTO;
import app.stage.presentation.dto.StageDTO;
import app.stage.presentation.dto.StageUpdateDTO;
import app.project.domain.Project;
import app.shared.data.IReadDAO;
import app.stage.data.IStageDAO;
import app.utils.ValidationUtil;
import app.stage.data.StageMapper;
import app.exceptions.ApiException;
import jakarta.persistence.EntityNotFoundException;

import java.util.List;

public class StageService implements IStageService
{
    private final IStageDAO stageDAO;
    private final IReadDAO<Project> projectReader;

    public StageService(IStageDAO stageDAO, IReadDAO<Project> projectReader)
    {
        this.stageDAO = stageDAO;
        this.projectReader = projectReader;
    }

    @Override
    public StageDTO create(StageCreateDTO dto)
    {
        ValidationUtil.validateNotNull(dto, "Stage");
        ValidationUtil.validateId(dto.projectId());
        validateName(dto.name());

        Project project;
        try
        {
            project = projectReader.get(dto.projectId());
        }
        catch (EntityNotFoundException e)
        {
            throw new ApiException(404, "Project not found with id: " + dto.projectId());
        }
        Stage created = stageDAO.create(new Stage(dto.name().trim(), project));
        return toDTO(created);
    }

    @Override
    public StageDTO get(Long id)
    {
        return toDTO(getExistingStage(id));
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

        Stage stage = getExistingStage(dto.id());
        stage.update(dto.name().trim());
        stageDAO.update(stage);
        return get(dto.id());
    }

    @Override
    public StageDTO update(Long id, StageUpdateDTO dto)
    {
        ValidationUtil.validateId(id);
        ValidationUtil.validateNotNull(dto, "Stage");
        validateName(dto.name());

        Stage stage = getExistingStage(id);
        stage.update(dto.name().trim());
        stageDAO.update(stage);
        return get(id);
    }

    @Override
    public void delete(Long id)
    {
        ValidationUtil.validateId(id);
        getExistingStage(id);
        stageDAO.delete(id);
    }

    private void validateName(String name)
    {
        ValidationUtil.validateNotBlank(name, "Stage name");
    }

    private Stage getExistingStage(Long id)
    {
        try
        {
            return stageDAO.get(id);
        }
        catch (EntityNotFoundException e)
        {
            throw new ApiException(404, "Stage not found with id: " + id);
        }
    }

    private StageDTO toDTO(Stage stage)
    {
        return StageMapper.toDTO(stage);
    }
}
