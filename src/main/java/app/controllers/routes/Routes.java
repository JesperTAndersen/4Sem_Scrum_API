package app.controllers.routes;

import app.controllers.SecurityController;
import io.javalin.apibuilder.EndpointGroup;

import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

public class Routes
{
    private static final String API_VERSION = "api/v1";
    private final SecurityRoutes securityRoutes;

    public Routes(SecurityController securityController)
    {
        this.securityRoutes = new SecurityRoutes(securityController);
    }

    public EndpointGroup getRoutes()
    {
        return () ->
        {
            get("/", ctx -> ctx.status(200).json(Map.of("message", "Welcome to the Scrum API!")));

            path(API_VERSION, () ->
            {
                securityRoutes.getRoutes().addEndpoints();
            });
        };
    }

    public static String getApiVersion()
    {
        return API_VERSION;
    }
}