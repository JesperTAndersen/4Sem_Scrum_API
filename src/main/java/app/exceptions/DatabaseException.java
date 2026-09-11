package app.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseException extends RuntimeException
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseException.class);

    public DatabaseException(String message, Throwable cause)
    {
        super(message, cause);
        LOGGER.error("Database error: {} | Cause: {}", message, cause.getMessage(), cause);
    }
}
