package app.exceptions;

public class TokenVerificationException extends RuntimeException
{
    public TokenVerificationException(String message)
    {
        super(message);
    }

    public TokenVerificationException(String message, Throwable cause)
    {
        super(message, cause);
    }
}