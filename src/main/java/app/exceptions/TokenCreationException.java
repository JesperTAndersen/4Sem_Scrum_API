package app.exceptions;

import io.javalin.http.HttpStatus;

public class TokenCreationException extends ApiException
{
    public TokenCreationException(String message, Throwable cause)
    {
        super(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), message, cause);
    }
}