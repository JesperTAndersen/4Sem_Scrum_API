package app.routes;

import app.controllers.interfaces.generic.ICrudController;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class CompetenceRoutes
{
    private final ICrudController competenceController;

    public CompetenceRoutes(ICrudController competenceController)
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
            delete("/{id}", competenceController::delete);
        });
    }
}
