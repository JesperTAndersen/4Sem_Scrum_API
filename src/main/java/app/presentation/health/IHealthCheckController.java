package app.presentation.health;

import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;

public interface IHealthCheckController
{
    void healthCheck(@NotNull Context ctx);
}
