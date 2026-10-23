package app.exceptions;

import io.javalin.http.HttpStatus;

public class ConflictException extends ApiException
{
    public ConflictException(String message)
    {
        super(HttpStatus.CONFLICT.getCode(), message);
    }

    public ConflictException(String message, Throwable cause)
    {
        super(HttpStatus.CONFLICT.getCode(), message, cause);
    }
}
