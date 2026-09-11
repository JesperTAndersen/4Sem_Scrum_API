package app.controllers;

import app.dtos.CompetenceDTO;
import app.services.interfaces.ICompetenceService;
import io.javalin.http.Context;

public class CompetenceController
{
    private final ICompetenceService iCompetenceService;

    public CompetenceController(ICompetenceService iCompetenceService)
    {
        this.iCompetenceService = iCompetenceService;
    }

    public void create(Context ctx)
    {
        CompetenceDTO body = ctx.bodyValidator(CompetenceDTO.class)
                .check(competence -> competence.name() != null, "Name is required")
                .check(competence -> competence.rate() != null, "Rate is required")
                .get();

        CompetenceDTO dto = new CompetenceDTO(null, body.name(), body.rate());
        ctx.status(201).json(iCompetenceService.create(dto));
    }

    public void getAll(Context ctx)
    {
        ctx.json(iCompetenceService.getAll());
    }

    public void get(Context ctx)
    {
        Long id = Long.parseLong(ctx.pathParam("id"));
        ctx.json(iCompetenceService.get(id));
    }

    public void update(Context ctx)
    {
        Long id = Long.parseLong(ctx.pathParam("id"));
        CompetenceDTO body = ctx.bodyValidator(CompetenceDTO.class)
                .check(competence -> competence.name() != null, "Name is required")
                .check(competence -> competence.rate() != null, "Rate is required")
                .get();

        CompetenceDTO dto = new CompetenceDTO(id, body.name(), body.rate());
        ctx.json(iCompetenceService.update(dto));
    }

    public void delete(Context ctx)
    {
        Long id = Long.parseLong(ctx.pathParam("id"));
        iCompetenceService.delete(id);
        ctx.status(204);
    }
}
