package app.config;

import java.util.Map;

import app.exceptions.ConfigurationException;
import app.exceptions.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.security.presentation.ISecurityController;
import app.exceptions.ApiException;
import app.competence.presentation.CompetenceRoutes;
import app.presentation.health.HealthCheckRoute;
import app.project.presentation.ProjectRoutes;
import app.presentation.Routes;
import app.security.presentation.SecurityRoutes;
import app.stage.presentation.StageRoutes;
import app.task.presentation.TaskRoutes;
import app.user.presentation.UserRoutes;
import app.utils.ExecutionTimer;
import app.utils.JWTUtil;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import jakarta.persistence.EntityManagerFactory;

public class ApplicationConfig
{
    private static final Logger log = LoggerFactory.getLogger(ApplicationConfig.class);

    public static Javalin startServer(int port)
    {
        ExecutionTimer.start();
        JWTUtil.validate();
        DependencyContainer dependencyContainer = DependencyContainer.getInstance();
        Routes routes = buildRoutes(dependencyContainer);

        Javalin app = Javalin.create(config ->
        {
            configureRoutes(config, routes, dependencyContainer.getSecurityController());
            configureSecurity(config, dependencyContainer);
            configureCors(config);
            configureExceptions(config);
            configureJackson(config, dependencyContainer);
            configureLogger(config);
        }).start(port);

        ExecutionTimer.finish("Scrum Project \"Estimo\" ready on port " + port);
        return app;
    }

    // For test instances
    public static Javalin startServer(int port, EntityManagerFactory emf)
    {
        JWTUtil.validate();
        DependencyContainer dependencyContainer = DependencyContainer.getTestInstance(emf);
        Routes routes = buildRoutes(dependencyContainer);

        Javalin app = Javalin.create(config ->
        {
            configureRoutes(config, routes, dependencyContainer.getSecurityController());
            configureSecurity(config, dependencyContainer);
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
                new HealthCheckRoute(dependencyContainer.getHealthCheckController()),
                new UserRoutes(dependencyContainer.getUserController()),
                new StageRoutes(dependencyContainer.getStageController()),
                new TaskRoutes(dependencyContainer.getTaskController()),
                new CompetenceRoutes(dependencyContainer.getCompetenceController()),
                new ProjectRoutes((dependencyContainer.getProjectController())),
                new SecurityRoutes(dependencyContainer.getSecurityController())
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

    private static void configureRoutes(JavalinConfig config, Routes routes, ISecurityController securityController)
    {
        config.bundledPlugins.enableRouteOverview("/routes");
        config.routes.apiBuilder(routes.getRoutes());
    }

    private static void configureSecurity(JavalinConfig config, DependencyContainer dependencyContainer)
    {
        config.routes.beforeMatched(dependencyContainer.getSecurityController()::authenticate);
        config.routes.beforeMatched(dependencyContainer.getSecurityController()::authorize);
    }

    private static void configureExceptions(JavalinConfig config)
    {
        config.routes.exception(ApiException.class, (e, ctx) ->
        {
            boolean isServerFault = e.getCode() >= 500;
            ErrorResponse error = isServerFault
                    ? ErrorResponse.of(e.getCode(), "Internal server error", ctx.path())
                    : ErrorResponse.of(e.getCode(), e.getMessage(), ctx.path());

            if (isServerFault)
            {
                log.error("{} {} - [{}] {}", ctx.method(), ctx.path(), error.errorId(), e.getMessage(), e);
            }
            else
            {
                log.warn("{} {} - [{}] {}", ctx.method(), ctx.path(), error.errorId(), e.getMessage());
            }

            ctx.status(e.getCode()).json(error);
        });

        config.routes.exception(ConfigurationException.class, (e, ctx) ->
        {
            ErrorResponse error = ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "Internal server error", ctx.path());
            log.error("{} {} - [{}] Configuration error: {}", ctx.method(), ctx.path(), error.errorId(), e.getMessage(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR.getCode()).json(error);
        });

        config.routes.exception(NumberFormatException.class, (e, ctx) ->
        {
            ErrorResponse error = ErrorResponse.of(HttpStatus.BAD_REQUEST.getCode(),
                    "Invalid ID format: expected a number", ctx.path());
            log.warn("{} {} - [{}] Invalid number format: {}", ctx.method(), ctx.path(), error.errorId(), e.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST.getCode()).json(error);
        });

        config.routes.exception(Exception.class, (e, ctx) ->
        {
            ErrorResponse error = ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "Internal server error", ctx.path());
            log.error("{} {} - [{}] Unhandled exception", ctx.method(), ctx.path(), error.errorId(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR.getCode()).json(error);
        });
    }

    private static void configureLogger(JavalinConfig config)
    {
        config.requestLogger.http((ctx, ms) ->
        {
            if (ctx.path().equals(Routes.getAPI_VERSION() + "/health"))
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
