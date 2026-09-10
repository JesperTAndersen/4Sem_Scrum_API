package app.controllers;

import app.DTOs.CompetenceDTO;
import app.services.interfaces.CompetenceService;
import io.javalin.http.Context;

public class CompetenceController
{
    private final CompetenceService competenceService;

    public CompetenceController(CompetenceService competenceService)
    {
        this.competenceService = competenceService;
    }

    public void create(Context ctx)
    {
        CompetenceDTO body = ctx.bodyValidator(CompetenceDTO.class)
                .check(competence -> competence.name() != null, "Name is required")
                .check(competence -> competence.rate() != null, "Rate is required")
                .get();

        CompetenceDTO dto = new CompetenceDTO(null, body.name(), body.rate());
        ctx.status(201).json(competenceService.create(dto));
    }

    public void getAll(Context ctx)
    {
        ctx.json(competenceService.getAll());
    }

    public void get(Context ctx)
    {
        Long id = Long.parseLong(ctx.pathParam("id"));
        ctx.json(competenceService.get(id));
    }

    public void update(Context ctx)
    {
        Long id = Long.parseLong(ctx.pathParam("id"));
        CompetenceDTO body = ctx.bodyValidator(CompetenceDTO.class)
                .check(competence -> competence.name() != null, "Name is required")
                .check(competence -> competence.rate() != null, "Rate is required")
                .get();

        CompetenceDTO dto = new CompetenceDTO(id, body.name(), body.rate());
        ctx.json(competenceService.update(dto));
    }

    public void delete(Context ctx)
    {
        Long id = Long.parseLong(ctx.pathParam("id"));
        competenceService.delete(id);
        ctx.status(204);
    }
}
