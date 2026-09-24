package app.task.presentation;

import app.task.presentation.dto.TaskCreateDTO;
import app.task.presentation.dto.TaskUpdateDTO;
import app.task.domain.ITaskService;
import app.shared.presentation.ICrudController;
import app.utils.RequestUtil;
import io.javalin.http.Context;

import java.util.Objects;

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
                .check(task -> task.minimumDurationInDays() != null, "Task minimum duration is required")
                .check(task -> task.competenceId() != null, "Competence is required")
                .check(task -> task.estimate() != null, "Task estimate is required")
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
                .get();
        ctx.status(200).json(taskService.update(id, dto));
    }

    @Override
    public void delete(Context ctx)
    {
        taskService.delete(RequestUtil.requirePathId(ctx, "id"));
        ctx.status(204);
    }

    public void addPredecessor(Context ctx)
    {
        Long taskId = RequestUtil.requirePathId(ctx, "taskId");
        Long predecessorId = RequestUtil.requirePathId(ctx, "predecessorId");
        taskService.addPredecessor(taskId, predecessorId);
        ctx.status(204);
    }

    public void removePredecessor(Context ctx)
    {
        Long taskId = RequestUtil.requirePathId(ctx, "taskId");
        Long predecessorId = RequestUtil.requirePathId(ctx, "predecessorId");
        taskService.removePredecessor(taskId, predecessorId);
        ctx.status(204);
    }
}
