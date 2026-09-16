package app.project.domain;

import app.project.presentation.dto.CreateProjectDTO;
import app.project.presentation.dto.ProjectDTO;
import app.project.presentation.dto.UpdateProjectDTO;
import app.security.presentation.dto.AuthenticatedUser;
import app.project.domain.Project;
import app.project.domain.ProjectStatus;
import app.exceptions.ApiException;
import app.project.data.ProjectMapper;
import app.project.data.IProjectDAO;
import app.project.domain.IProjectService;
import app.shared.data.IReadDAO;
import app.user.data.IUserDAO;
import app.user.domain.User;
import app.utils.ValidationUtil;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDate;
import java.util.List;

public class ProjectService implements IProjectService
{
    private final IProjectDAO projectDAO;
    private final IReadDAO<User> userDAO;

    public ProjectService(IProjectDAO projectDAO, IReadDAO<User> userDAO)
    {
        this.projectDAO = projectDAO;
        this.userDAO = userDAO;
    }

    @Override
    public ProjectDTO create(AuthenticatedUser authUser, CreateProjectDTO dto)
    {
        validateAuthenticatedUser(authUser);
        validateCreate(dto);

        Project project = ProjectMapper.toEntity(
                dto,
                ProjectStatus.DRAFT,
                getAuthenticatedUser(authUser)
        );

        Project createdProject = projectDAO.create(project);
        return ProjectMapper.toDTO(createdProject);
    }

    @Override
    public ProjectDTO get(Long id)
    {
        return ProjectMapper.toDTO(getExistingProject(id));
    }

    @Override
    public List<ProjectDTO> getAll()
    {
        return projectDAO.getAll().stream()
                .map(ProjectMapper::toDTO)
                .toList();
    }

    @Override
    public ProjectDTO update(AuthenticatedUser authUser, Long id, UpdateProjectDTO dto)
    {
        validateAuthenticatedUser(authUser);
        ValidationUtil.validateId(id);
        validateUpdate(dto);

        Project existingProject = getExistingProject(id);

        Project project = ProjectMapper.toEntity(
                dto,
                existingProject,
                getAuthenticatedUser(authUser)
        );

        Project updatedProject = projectDAO.update(project);
        return ProjectMapper.toDTO(updatedProject);
    }

    @Override
    public void delete(Long id)
    {
        ValidationUtil.validateId(id);

        try
        {
            projectDAO.delete(id);
        }
        catch (EntityNotFoundException e)
        {
            throw new ApiException(404, "Project not found with id: " + id);
        }
    }

    private Project getExistingProject(Long id)
    {
        ValidationUtil.validateId(id);

        try
        {
            return projectDAO.get(id);
        }
        catch (EntityNotFoundException e)
        {
            throw new ApiException(404, "Project not found with id: " + id);
        }
    }

    private void validateCreate(CreateProjectDTO dto)
    {
        if (dto == null)
        {
            throw new ApiException(400, "Project payload is required");
        }

        validateProjectFields(dto.title(), dto.description(), dto.startDate(), dto.deadline());
    }

    private void validateUpdate(UpdateProjectDTO dto)
    {
        if (dto == null)
        {
            throw new ApiException(400, "Project payload is required");
        }

        validateProjectFields(dto.title(), dto.description(), dto.startDate(), dto.deadline());

        if (dto.status() == null)
        {
            throw new ApiException(400, "Project status is required");
        }
    }

    private void validateProjectFields(String title, String description, LocalDate startDate, LocalDate deadline)
    {
        if (title == null || title.isBlank())
        {
            throw new ApiException(400, "Project title is required");
        }

        if (title.trim().length() > 250)
        {
            throw new ApiException(400, "Project title must be at most 250 characters");
        }

        if (description != null && description.length() > 500)
        {
            throw new ApiException(400, "Project description must be at most 500 characters");
        }

        if (startDate == null)
        {
            throw new ApiException(400, "Project start date is required");
        }

        if (deadline == null)
        {
            throw new ApiException(400, "Project deadline is required");
        }

        if (deadline.isBefore(startDate))
        {
            throw new ApiException(400, "Project deadline cannot be before start date");
        }
    }

    private void validateAuthenticatedUser(AuthenticatedUser authUser)
    {
        if (authUser == null || authUser.id() == null || authUser.email() == null || authUser.email().isBlank())
        {
            throw new ApiException(401, "Authenticated user is required");
        }
    }

    private User getAuthenticatedUser(AuthenticatedUser authUser)
    {
        User user = userDAO.get(authUser.id());
        if (user == null)
        {
            throw new ApiException(401, "Authenticated user is required");
        }
        return user;
    }
}
