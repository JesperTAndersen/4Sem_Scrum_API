package app.exceptions;

import java.time.Instant;
import java.util.UUID;

public record ErrorResponse(
        int status,
        String message,
        String path,
        String errorId,
        Instant timestamp)
{
    public static ErrorResponse of(int status, String message, String path)
    {
        return new ErrorResponse(status, message, path, UUID.randomUUID().toString(), Instant.now());
    }
}