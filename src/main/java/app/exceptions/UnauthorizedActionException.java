package app.exceptions;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class UnauthorizedActionException extends RuntimeException
{
    private static final Logger logger = LoggerFactory.getLogger(UnauthorizedActionException.class);
    public UnauthorizedActionException(String message) {
        super(message);
        logger.error("UnauthorizedActionException : {}", message);
    }
}
