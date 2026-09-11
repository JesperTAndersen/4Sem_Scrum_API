package app.routes;

import io.javalin.apibuilder.EndpointGroup;

public class ApiRoutes
{
    private final UserRoutes userRoutes;

    public ApiRoutes( UserRoutes userRoute)
    {
        this.userRoutes = userRoute;
    }

    public EndpointGroup getRoutes()
    {
        return () ->
        {
            userRoutes.getRoutes().addEndpoints();
        };
    }
}
