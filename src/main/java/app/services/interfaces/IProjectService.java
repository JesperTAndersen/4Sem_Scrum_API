package app.services.interfaces;

import app.DTOs.project.CreateProjectDTO;
import app.DTOs.project.ProjectDTO;
import app.DTOs.project.UpdateProjectDTO;
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
