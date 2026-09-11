package app.config;

import app.routes.HealthCheckRoute;
import app.routes.Routes;
import app.exceptions.ApiException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ApplicationConfig
{
    private static final Logger log = LoggerFactory.getLogger(ApplicationConfig.class);

    public static Javalin startServer(int port)
    {
        DependencyContainer dependencyContainer = DependencyContainer.getInstance();
        Routes routes = buildRoutes(dependencyContainer);

        Javalin app = Javalin.create(config ->
        {
            configureRoutes(config, routes);
            configureCors(config);
            configureExceptions(config);
            configureJackson(config, dependencyContainer);
            configureLogger(config);
        }).start(port);

        return app;
    }

    // For test instances
    public static Javalin startServer(int port, EntityManagerFactory emf)
    {
        DependencyContainer dependencyContainer = DependencyContainer.getTestInstance(emf);
        Routes routes = buildRoutes(dependencyContainer);

        Javalin app = Javalin.create(config ->
        {
            configureRoutes(config, routes);
            configureCors(config);
            configureExceptions(config);
            configureJackson(config, dependencyContainer);
            configureLogger(config);
        }).start(port);

        return app;
    }

    public static void stopServer(Javalin app)
    {
        log.info("Scrum Project shutting down");
        app.stop();
    }

    private static Routes buildRoutes(DependencyContainer dependencyContainer)
    {
        return new Routes(
                new HealthCheckRoute(dependencyContainer.getHealthCheckController())
        );
    }

    private static void configureCors(JavalinConfig config)
    {
        boolean isProduction = System.getenv("DEPLOYED") != null;

        config.bundledPlugins.enableCors(cors ->
        {
            cors.addRule(rule ->
            {
                if (isProduction)
                {
                    String allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
                    for (String origin : allowedOrigins.split(","))
                    {
                        rule.allowHost(origin.trim());
                    }
                }
                else
                {
                    rule.anyHost();
                }
            });
        });
    }

    private static void configureRoutes(JavalinConfig config, Routes routes)
    {
        config.bundledPlugins.enableRouteOverview("/routes");
        config.routes.apiBuilder(routes.getRoutes());
    }


    private static void configureExceptions(JavalinConfig config)
    {
        config.routes.exception(ApiException.class, (e, ctx) ->
        {
            log.warn("{} {} - {}", ctx.method(), ctx.path(), e.getMessage());
            ctx.status(e.getCode())
                    .json(Map.of("status", e.getCode(),
                            "message", e.getMessage()));
        });

        config.routes.exception(NumberFormatException.class, (e, ctx) ->
        {
            log.warn("{} {} - Invalid number format: {}", ctx.method(), ctx.path(), e.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST.getCode())
                    .json(Map.of("status", HttpStatus.BAD_REQUEST.getCode(),
                            "message", "Invalid ID format: expected a number"));
        });

        config.routes.exception(Exception.class, (e, ctx) ->
        {
            log.error("{} {} - Unhandled exception", ctx.method(), ctx.path(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR.getCode())
                    .json(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                            "message", "Internal server error"));
        });
    }

    private static void configureLogger(JavalinConfig config)
    {
        config.requestLogger.http((ctx, ms) ->
        {
            if (ctx.path().equals("/api/v1/health"))
            {
                return;
            }
            if (ctx.status().getCode() >= 500)
            {
                log.error("{} {} - {} ({}ms)", ctx.method(), ctx.path(), ctx.status(), ms.longValue());
            }
            else if (ctx.status().getCode() >= 400)
            {
                log.warn("{} {} - {} ({}ms)", ctx.method(), ctx.path(), ctx.status(), ms.longValue());
            }
            else
            {
                log.info("{} {} - {} ({}ms)", ctx.method(), ctx.path(), ctx.status(), ms.longValue());
            }
        });
    }

    private static void configureJackson(JavalinConfig config, DependencyContainer dependencyContainer)
    {
        config.jsonMapper(new JavalinJackson(dependencyContainer.getObjectMapper(), false));
    }
}