package app.employee.presentation;

import app.shared.presentation.ICrudController;
import io.javalin.http.Context;

public interface IEmployeeController extends ICrudController
{
    void setActive(Context ctx);

    void setInactive(Context ctx);
}
