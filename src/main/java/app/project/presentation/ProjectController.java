package app.project.presentation;

import app.project.presentation.dto.CreateProjectDTO;
import app.project.presentation.dto.ProjectDTO;
import app.project.presentation.dto.SlimProjectDTO;
import app.project.presentation.dto.UpdateProjectDTO;
import app.shared.presentation.ICrudController;
import app.security.presentation.dto.AuthenticatedUser;
import app.project.domain.IProjectService;
import app.utils.RequestUtil;
import app.utils.SecurityUtil;
import io.javalin.http.Context;

import java.util.List;
import java.util.Objects;

public class ProjectController implements ICrudController
{
    private final IProjectService projectService;

    public ProjectController(IProjectService projectService)
    {
        this.projectService = projectService;
    }

    @Override
    public void create(Context ctx)
    {
        AuthenticatedUser authUser = SecurityUtil.getAuthenticatedUser(ctx);

        CreateProjectDTO dto = ctx.bodyValidator(CreateProjectDTO.class)
                .check(Objects::nonNull, "Project payload cannot be null")
                .check(project -> project.title() != null && !project.title().isBlank(), "Project title is required")
                .check(project -> project.startDate() != null, "Project start date is required")
                .check(project -> project.deadline() != null, "Project deadline is required")
                .get();

        ProjectDTO project = projectService.create(authUser, dto);
        ctx.status(201).json(project);
    }

    @Override
    public void get(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        ProjectDTO project = projectService.get(id);
        ctx.status(200).json(project);
    }

    @Override
    public void getAll(Context ctx)
    {
        List<SlimProjectDTO> projects = projectService.getAll();
        ctx.status(200).json(projects);
    }

    @Override
    public void update(Context ctx)
    {
        AuthenticatedUser authUser = SecurityUtil.getAuthenticatedUser(ctx);
        Long id = RequestUtil.requirePathId(ctx, "id");

        UpdateProjectDTO dto = ctx.bodyValidator(UpdateProjectDTO.class)
                .check(Objects::nonNull, "Project payload cannot be null")
                .check(project -> project.title() != null && !project.title().isBlank(), "Project title is required")
                .check(project -> project.startDate() != null, "Project start date is required")
                .check(project -> project.deadline() != null, "Project deadline is required")
                .check(project -> project.status() != null, "Project status is required")
                .get();

        ProjectDTO project = projectService.update(authUser, id, dto);
        ctx.status(200).json(project);
    }

    @Override
    public void delete(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        projectService.delete(id);
        ctx.status(204);
    }
}
