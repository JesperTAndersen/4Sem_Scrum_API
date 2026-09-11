package app.controllers.implementations;

import app.controllers.interfaces.generic.ICrudController;
import app.dtos.stage.StageCreateDTO;
import app.dtos.stage.StageDTO;
import app.services.interfaces.IStageService;
import io.javalin.http.Context;

public class StageController implements ICrudController
{
    private final IStageService stageService;

    public StageController(IStageService stageService)
    {
        this.stageService = stageService;
    }

    public void post(Context ctx)
    {
        StageCreateDTO stageCreateDTO;
        StageDTO stageDTO;

        stageCreateDTO = ctx.bodyAsClass(StageCreateDTO.class);
        stageDTO = stageService.create(stageCreateDTO);

        ctx.status(201).json(stageDTO);
    }

    @Override
    public void get(Context ctx)
    {

    }

    @Override
    public void getAll(Context ctx)
    {

    }

    @Override
    public void create(Context ctx)
    {

    }

    @Override
    public void update(Context ctx)
    {

    }

    @Override
    public void delete(Context ctx)
    {

    }
}
