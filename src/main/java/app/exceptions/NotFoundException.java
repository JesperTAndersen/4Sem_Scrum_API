package app.exceptions;

import io.javalin.http.HttpStatus;

public class NotFoundException extends ApiException
{
    public NotFoundException(String message)
    {
        super(HttpStatus.NOT_FOUND.getCode(), message);
    }
}
