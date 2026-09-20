package app.exceptions;

import io.javalin.http.HttpStatus;

public class ValidationException extends ApiException
{
    public ValidationException(String message)
    {
        super(HttpStatus.BAD_REQUEST.getCode(), message);
    }
}
