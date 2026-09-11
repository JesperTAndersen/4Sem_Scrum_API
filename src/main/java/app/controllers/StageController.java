package app.controllers;

import app.DTOs.StageCreateDTO;
import app.DTOs.StageDTO;
import app.services.StageService;
import io.javalin.http.Context;

public class StageController
{
    private StageService stageService;

    public StageController(StageService stageService)
    {
        this.stageService = stageService;
    }

    public void post(Context ctx)
    {
        StageCreateDTO stageCreateDTO;
        StageDTO stageDTO;

        stageCreateDTO = ctx.bodyAsClass(StageCreateDTO.class);
        stageDTO = stageService.createStage(stageCreateDTO);

        ctx.status(201).json(stageDTO);
    }
}
