package app.exceptions;

import io.javalin.http.HttpStatus;

public class ConfigurationException extends ApiException
{
    public ConfigurationException(String message)
    {
        super(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), message);
    }

    public ConfigurationException(String message, Throwable cause)
    {
        super(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), message, cause);
    }
}
