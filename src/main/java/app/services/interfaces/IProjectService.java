package app.services.interfaces;

import app.dtos.project.CreateProjectDTO;
import app.dtos.project.ProjectDTO;
import app.dtos.project.UpdateProjectDTO;
import app.dtos.security.AuthenticatedUser;

import java.util.List;

public interface IProjectService
{
    ProjectDTO create(AuthenticatedUser authUser, CreateProjectDTO dto);

    ProjectDTO get(Long id);

    List<ProjectDTO> getAll();

    ProjectDTO update(AuthenticatedUser authUser, Long id, UpdateProjectDTO dto);

    void delete(Long id);
}
