package app.exceptions;

import io.javalin.http.HttpStatus;

public class ForbiddenException extends ApiException
{
    public ForbiddenException(String message)
    {
        super(HttpStatus.FORBIDDEN.getCode(), message);
    }

    public ForbiddenException(String message, Throwable cause)
    {
        super(HttpStatus.FORBIDDEN.getCode(), message, cause);
    }
}
