package app.controllers.interfaces;

import io.javalin.http.Context;

public interface ISecurityController
{
    void login(Context ctx);

    void register(Context ctx);

    void authenticate(Context ctx) throws Exception;

    void authorize(Context ctx) throws Exception;
}

