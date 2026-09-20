package app.exceptions;

import io.javalin.http.HttpStatus;

public class DatabaseException extends ApiException
{
    public DatabaseException(String message)
    {
        super(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), message);
    }

    public DatabaseException(String message, Throwable cause)
    {
        super(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), message, cause);
    }
}
