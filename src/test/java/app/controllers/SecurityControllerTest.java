package app.controllers;

import app.controllers.implementations.SecurityController;
import app.dtos.security.AuthenticatedUser;
import app.dtos.security.LoginRequestDTO;
import app.dtos.user.CreateUserRequestDTO;
import app.enums.Role;
import app.exceptions.ApiException;
import app.services.interfaces.ISecurityService;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

class SecurityControllerTest
{
    @Test
    void protectedEndpointRejectsUnauthenticatedRequest()
    {
        SecurityController securityController = new SecurityController(new NoOpSecurityService());
        Javalin app = Javalin.create(config ->
        {
            config.routes.beforeMatched(securityController::authenticate);
            config.routes.beforeMatched(securityController::authorize);
            config.routes.exception(ApiException.class,
                    (exception, ctx) -> ctx.status(exception.getCode()).json(Map.of("message", exception.getMessage())));
            config.routes.get("/protected", ctx -> ctx.status(200), Role.EMPLOYEE);
        }).start(0);

        try
        {
            given()
                    .port(app.port())
                    .when()
                    .get("/protected")
                    .then()
                    .statusCode(401)
                    .body("message", is("Authorization header is missing"));
        }
        finally
        {
            app.stop();
        }
    }

    private static final class NoOpSecurityService implements ISecurityService
    {
        @Override
        public AuthenticatedUser register(CreateUserRequestDTO request)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String login(LoginRequestDTO request)
        {
            throw new UnsupportedOperationException();
        }
    }
}
