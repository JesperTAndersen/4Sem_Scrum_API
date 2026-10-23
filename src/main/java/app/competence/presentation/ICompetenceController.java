package app.competence.presentation;

import app.shared.presentation.ICrudController;
import io.javalin.http.Context;

public interface ICompetenceController extends ICrudController
{
    void setActive(Context ctx);

    void setInactive(Context ctx);
}
