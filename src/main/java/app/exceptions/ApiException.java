package app.exceptions;

public abstract class ApiException extends RuntimeException
{
    private final int code;

    protected ApiException(int code, String message)
    {
        super(message);
        this.code = code;
    }

    protected ApiException(int code, String message, Throwable cause)
    {
        super(message, cause);
        this.code = code;
    }

    public int getCode()
    {
        return code;
    }
}