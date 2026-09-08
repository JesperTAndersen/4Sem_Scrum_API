package app.controllers;

import io.javalin.http.Context;

public class SecurityController
{
    public SecurityController()
    {
    }

    public void healthCheck(Context ctx)
    {
        ctx.status(200).json("{\"msg\": \"API is up and running\"}");
    }
}
