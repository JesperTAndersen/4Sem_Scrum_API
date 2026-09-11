package app.services;

import app.persistence.implementations.StageDAO;
import app.persistence.ProjectDAO;
import app.entities.Stage;
import app.dtos.stage.StageDTO;
import app.dtos.stage.StageCreateDTO;

public class StageService
{
    private StageDAO stageDAO;
    private ProjectDAO projectDAO;

    public StageService(StageDAO stageDAO, ProjectDAO projectDAO)
    {
        this.stageDAO = stageDAO;
        this.projectDAO = projectDAO;
    }

    /* TODO: check project ownership */
    public StageDTO createStage(StageCreateDTO dto)
    {
        Project project;
        Stage stage;

        project = projectDAO.read(dto.projectId());
        assert (project != null);
        stage = new Stage(dto.name(), project);
        stageDAO.create(stage);
        assert (stage.id() != null);
        return toDTO(stage);
    }

    StageDTO toDTO(Stage stage)
    {
        return new StageDTO(stage.id(), stage.name);
    }
}
