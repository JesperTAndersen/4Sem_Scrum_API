package app.task.presentation;

import java.util.List;
import java.util.Objects;

import app.shared.presentation.ICrudController;
import app.task.domain.ITaskService;
import app.task.presentation.dto.TaskCompetenceDTO;
import app.task.presentation.dto.TaskCreateDTO;
import app.task.presentation.dto.TaskUpdateDTO;
import app.utils.RequestUtil;
import io.javalin.http.Context;

public class TaskController implements ICrudController
{
    private final ITaskService taskService;

    public TaskController(ITaskService taskService)
    {
        this.taskService = taskService;
    }

    @Override
    public void create(Context ctx)
    {
        TaskCreateDTO dto = ctx.bodyValidator(TaskCreateDTO.class)
                .check(Objects::nonNull, "Task payload is required")
                .check(task -> task.stageId() != null, "Stage id is required")
                .check(task -> task.name() != null && !task.name().isBlank(), "Task name is required")
                .get();
        ctx.status(201).json(taskService.create(dto));
    }

    @Override
    public void get(Context ctx)
    {
        ctx.status(200).json(taskService.get(RequestUtil.requirePathId(ctx, "id")));
    }

    @Override
    public void getAll(Context ctx)
    {
        ctx.status(200).json(taskService.getAll());
    }

    @Override
    public void update(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        TaskUpdateDTO dto = ctx.bodyValidator(TaskUpdateDTO.class)
                .check(Objects::nonNull, "Task payload is required")
                .check(task -> task.name() != null && !task.name().isBlank(), "Task name is required")
                .check(task -> task.estimate() != null, "Task estimate is required")
                .get();
        ctx.status(200).json(taskService.update(id, dto));
    }

    @Override
    public void delete(Context ctx)
    {
        taskService.delete(RequestUtil.requirePathId(ctx, "id"));
        ctx.status(204);
    }

    public void getAllCompetence(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        List<TaskCompetenceDTO> list = taskService.getAllCompetence(id);
        ctx.status(200).json(list);
    }

    public void addCompetence(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        TaskCompetenceDTO dto = ctx.bodyAsClass(TaskCompetenceDTO.class);
        taskService.addCompetence(id, dto);
        ctx.status(204);
    }

    public void remCompetence(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        Long competenceId = RequestUtil.requirePathId(ctx, "competenceId");
        taskService.remCompetence(id, competenceId);
        ctx.status(204);
    }
}
