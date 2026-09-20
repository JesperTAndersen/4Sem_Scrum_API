package app.stage.presentation;

import app.stage.presentation.dto.StageCreateDTO;
import app.stage.presentation.dto.StageUpdateDTO;
import app.stage.domain.IStageService;
import app.shared.presentation.ICrudController;
import app.utils.RequestUtil;
import io.javalin.http.Context;

import java.util.Objects;

public class StageController implements ICrudController
{
    private final IStageService stageService;

    public StageController(IStageService stageService)
    {
        this.stageService = stageService;
    }

    @Override
    public void create(Context ctx)
    {
        StageCreateDTO dto = ctx.bodyValidator(StageCreateDTO.class)
                .check(Objects::nonNull, "Stage payload is required")
                .check(stage -> stage.projectId() != null, "Project id is required")
                .check(stage -> stage.name() != null && !stage.name().isBlank(), "Stage name is required")
                .get();
        ctx.status(201).json(stageService.create(dto));
    }

    @Override
    public void get(Context ctx)
    {
        ctx.status(200).json(stageService.get(RequestUtil.requirePathId(ctx, "id")));
    }

    @Override
    public void getAll(Context ctx)
    {
        ctx.status(200).json(stageService.getAll());
    }

    @Override
    public void update(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        StageUpdateDTO dto = ctx.bodyValidator(StageUpdateDTO.class)
                .check(Objects::nonNull, "Stage payload is required")
                .check(stage -> stage.name() != null && !stage.name().isBlank(), "Stage name is required")
                .get();
        ctx.status(200).json(stageService.update(id, dto));
    }

    @Override
    public void delete(Context ctx)
    {
        stageService.delete(RequestUtil.requirePathId(ctx, "id"));
        ctx.status(204);
    }
}
