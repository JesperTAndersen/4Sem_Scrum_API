package app.utils;

import app.exceptions.BadRequestException;
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

    public static LocalDate requireQueryDate(Context ctx, String param)
    {
        String value = ctx.queryParam(param);
        if (value == null || value.isBlank())
        {
            throw new BadRequestException(param + " is required");
        }
        try
        {
            return LocalDate.parse(value);
        }
        catch (DateTimeParseException e)
        {
            throw new BadRequestException(param + " must be a valid date in YYYY-MM-DD format");
        }
    }
}
