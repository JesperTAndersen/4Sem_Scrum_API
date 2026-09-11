package app.controllers.routes;

import app.controllers.routes.UserRoute;
import io.javalin.apibuilder.EndpointGroup;

public class ApiRoutes
{
    private final UserRoute userRoute;

    public ApiRoutes( UserRoute userRoute)
    {
        this.userRoute = userRoute;
    }

    public EndpointGroup getRoutes()
    {
        return () ->
        {
            userRoute.getRoutes().addEndpoints();
        };
    }
}
