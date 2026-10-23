package app.security.presentation;

import app.config.ApiTestExtension;
import app.exceptions.ApiException;
import app.security.domain.ISecurityService;
import app.security.domain.Role;
import app.security.presentation.dto.AuthenticatedUser;
import app.security.presentation.dto.LoginRequestDTO;
import app.user.presentation.dto.CreateUserRequestDTO;
import app.utils.JWTUtil;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.javalin.Javalin;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.Date;
import java.util.Map;

import static app.config.ApiTestExtension.givenToken;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.restassured.RestAssured.given;

@ExtendWith(ApiTestExtension.class)
class SecurityControllerTest
{
    private static final String OTHER_SECRET = "another-secret-that-is-at-least-32-characters-long";

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/security/security-controller/authorize-access.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Authenticate and authorize - require a valid, unexpired token with the route's role")
    void authorizesRequests(String routeRole, String authorization, int expected, String reason)
    {
        SecurityController securityController = new SecurityController(new NoOpSecurityService());
        Javalin app = Javalin.create(config ->
        {
            config.routes.beforeMatched(securityController::authenticate);
            config.routes.beforeMatched(securityController::authorize);
            config.routes.exception(ApiException.class,
                    (exception, ctx) -> ctx.status(exception.getCode()).json(Map.of("message", exception.getMessage())));
            config.routes.apiBuilder(() ->
            {
                get("/anyone", ctx -> ctx.status(200), Role.ANYONE);
                get("/authenticated", ctx -> ctx.status(200));
                get("/employee", ctx -> ctx.status(200), Role.EMPLOYEE);
            });
        });
        app.start(0);

        try
        {
            RequestSpecification request = given().baseUri("http://localhost").basePath("").port(app.port());
            String header = authorizationHeader(authorization);
            if (header != null)
            {
                request.header("Authorization", header);
            }

            request.when().get("/" + routeRole.toLowerCase()).then().statusCode(expected);
        }
        finally
        {
            app.stop();
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/security/security-controller/register-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Register - rejects an invalid request")
    void registerValidatesRequest(String body, int expected, String reason)
    {
        send("/auth/register", body, expected);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/security/security-controller/login-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Login - rejects an invalid request and wrong credentials")
    void loginValidatesRequest(String body, int expected, String reason)
    {
        send("/auth/login", body, expected);
    }

    private void send(String path, String body, int expected)
    {
        RequestSpecification request = givenToken(null);
        if (body != null)
        {
            request.body(body);
        }

        request.when().post(path).then().statusCode(expected);
    }

    private String authorizationHeader(String authorization)
    {
        return switch (authorization)
        {
            case "NONE" -> null;
            case "BEARER_ONLY" -> "Bearer";
            case "NOT_A_JWT" -> "Bearer not-a-jwt";
            case "WRONG_SIGNATURE" -> "Bearer " + signedToken(OTHER_SECRET, new Date(System.currentTimeMillis() + 60_000));
            case "EXPIRED" -> "Bearer " + signedToken(System.getenv("JWT_SECRET"), new Date(System.currentTimeMillis() - 60_000));
            case "EMPLOYEE" -> "Bearer " + JWTUtil.createToken(2L, "employee@example.com", Role.EMPLOYEE);
            case "PROJECT_MANAGER" -> "Bearer " + JWTUtil.createToken(1L, "manager@example.com", Role.PROJECT_MANAGER);
            default -> throw new IllegalArgumentException("Unknown authorization: " + authorization);
        };
    }

    // JWTUtil only creates valid tokens, so expired and foreign-signed tokens are built here.
    private String signedToken(String secret, Date expirationTime)
    {
        try
        {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("employee@example.com")
                    .issuer(System.getenv("JWT_ISSUER"))
                    .issueTime(new Date(expirationTime.getTime() - 120_000))
                    .claim("id", 2L)
                    .claim("role", Role.EMPLOYEE.name())
                    .expirationTime(expirationTime)
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        }
        catch (JOSEException e)
        {
            throw new AssertionError("Could not sign test token", e);
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
