package app.utils;

import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class RequestUtil
{
    private RequestUtil()
    {
    }

    public static Long requirePathId(Context ctx, String param)
    {
        return ctx.pathParamAsClass(param, Long.class)
                .check(i -> i > 0, param + " must be positive")
                .get();
    }


}
