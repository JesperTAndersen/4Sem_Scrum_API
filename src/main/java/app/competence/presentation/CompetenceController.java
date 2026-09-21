package app.competence.presentation;

import app.competence.presentation.dto.CompetenceDTO;
import app.competence.presentation.dto.CompetenceCreateDTO;
import app.competence.presentation.dto.CompetenceUpdateDTO;
import app.competence.domain.ICompetenceService;
import app.utils.RequestUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class CompetenceController implements ICompetenceController
{
    private final ICompetenceService iCompetenceService;

    public CompetenceController(ICompetenceService iCompetenceService)
    {
        this.iCompetenceService = iCompetenceService;
    }

    public void create(Context ctx)
    {
        CompetenceCreateDTO body = ctx.bodyValidator(CompetenceCreateDTO.class)
                .check(competence -> competence.name() != null, "Name is required")
                .check(competence -> competence.rate() != null, "Rate is required")
                .get();

        ctx.status(HttpStatus.CREATED).json(iCompetenceService.create(body));
    }

    public void getAll(Context ctx)
    {
        ctx.json(iCompetenceService.getAll());
    }

    public void get(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        ctx.json(iCompetenceService.get(id));
    }

    public void update(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        CompetenceUpdateDTO body = ctx.bodyValidator(CompetenceUpdateDTO.class)
                .check(competence -> competence.name() != null, "Name is required")
                .check(competence -> competence.rate() != null, "Rate is required")
                .get();

        ctx.json(iCompetenceService.update(id, body));
    }

    public void delete(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        iCompetenceService.delete(id);
        ctx.status(HttpStatus.NO_CONTENT);
    }

    @Override
    public void setActive(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        iCompetenceService.setActive(id, true);
        ctx.status(HttpStatus.NO_CONTENT);
    }

    @Override
    public void setInactive(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        iCompetenceService.setActive(id, false);
        ctx.status(HttpStatus.NO_CONTENT);
    }
}
