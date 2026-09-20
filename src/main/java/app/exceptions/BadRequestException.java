package app.exceptions;

import io.javalin.http.HttpStatus;

public class BadRequestException extends ApiException
{
    public BadRequestException(String message)
    {
        super(HttpStatus.BAD_REQUEST.getCode(), message);
    }
}
