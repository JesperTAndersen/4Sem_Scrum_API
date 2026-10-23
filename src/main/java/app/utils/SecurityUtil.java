package app.utils;

import app.exceptions.UnauthorizedException;
import app.security.presentation.dto.AuthenticatedUser;
import io.javalin.http.Context;
import io.javalin.websocket.WsContext;

public class SecurityUtil
{
    private SecurityUtil(){}

    public static AuthenticatedUser getAuthenticatedUser(Context ctx)
    {
        AuthenticatedUser authUser = ctx.attribute("authUser");
        validateAuthenticatedUser(authUser);

        return authUser;
    }

    public static AuthenticatedUser getAuthenticatedUserWebSocket(WsContext wsCtx)
    {
        AuthenticatedUser authUser = wsCtx.attribute("authUser");
        validateAuthenticatedUser(authUser);

        return authUser;
    }

    public static AuthenticatedUser getOptionalAuthenticatedUser(Context ctx)
    {
        return ctx.attribute("authUser");
    }

    private static void validateAuthenticatedUser(AuthenticatedUser authUser)
    {
        if (authUser == null)
        {
            throw new UnauthorizedException("No authenticated user found");
        }

        if (authUser.id() == null || authUser.id() <= 0)
        {
            throw new UnauthorizedException("Authenticated user id is invalid");
        }

        if (authUser.email() == null || authUser.email().isBlank())
        {
            throw new UnauthorizedException("Authenticated user email is invalid");
        }
    }
}
