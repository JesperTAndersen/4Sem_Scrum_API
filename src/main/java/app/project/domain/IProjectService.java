package app.project.domain;

import app.project.presentation.dto.CreateProjectDTO;
import app.project.presentation.dto.ProjectDTO;
import app.project.presentation.dto.SlimProjectDTO;
import app.project.presentation.dto.UpdateProjectDTO;
import app.security.presentation.dto.AuthenticatedUser;

import java.util.List;

public interface IProjectService
{
    ProjectDTO create(AuthenticatedUser authUser, CreateProjectDTO dto);

    ProjectDTO get(Long id);

    List<SlimProjectDTO> getAll();

    ProjectDTO update(AuthenticatedUser authUser, Long id, UpdateProjectDTO dto);

    void delete(Long id);
}
