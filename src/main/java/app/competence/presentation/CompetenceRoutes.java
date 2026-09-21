package app.competence.presentation;

import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class CompetenceRoutes
{
    private final ICompetenceController competenceController;

    public CompetenceRoutes(ICompetenceController competenceController)
    {
        this.competenceController = competenceController;
    }

    public EndpointGroup getRoutes()
    {
        return () -> path("competences", () ->
        {
            get(competenceController::getAll);
            get("/{id}", competenceController::get);
            post(competenceController::create);
            put("/{id}", competenceController::update);
            patch("/{id}/activate",competenceController::setActive);
            patch("/{id}/deactivate",competenceController::setInactive);
            delete("/{id}", competenceController::delete);
        });
    }
}
