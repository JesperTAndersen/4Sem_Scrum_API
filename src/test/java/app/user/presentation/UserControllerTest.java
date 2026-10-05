package app.user.presentation;

import static app.config.ApiTestExtension.UNKNOWN_ID;
import static app.config.ApiTestExtension.givenManager;
import static app.config.ApiTestExtension.givenToken;
import static app.config.ApiTestExtension.readJson;

import app.config.ApiTestExtension;
import app.security.domain.Role;
import app.utils.JWTUtil;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(ApiTestExtension.class)
class UserControllerTest
{
    // Emails are unique, so every registered user and every changed email gets its own address.
    private static final AtomicInteger EMAIL_COUNTER = new AtomicInteger();
    private static final String PASSWORD = "Password1!";

    @ParameterizedTest(name = "[{index}] {5}")
    @CsvFileSource(resources = "/testcases/user/user-controller/endpoint-access.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("All endpoints - require an authenticated user, and changes to credentials require ownership")
    void enforcesEndpointAccess(String method, String path, String target, String token, int expected, String reason)
    {
        RegisteredUser actingUser = register();
        String targetId = switch (target)
        {
            case "OWN" -> String.valueOf(actingUser.id());
            case "OTHER" -> String.valueOf(register().id());
            case "UNKNOWN" -> String.valueOf(UNKNOWN_ID);
            default -> "";
        };

        RequestSpecification request = givenToken(token.equals("NONE") ? null : actingUser.token());
        String body = validBody(method, path);
        if (body != null)
        {
            request.body(body);
        }

        request.when().request(method, path.replace("{id}", targetId)).then().statusCode(expected);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/user/user-controller/path-id.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Endpoints with an id - reject an invalid path id")
    void rejectsInvalidPathId(String method, String path, int expected, String reason)
    {
        RequestSpecification request = givenManager();
        String body = validBody(method, path);
        if (body != null)
        {
            request.body(body);
        }

        request.when().request(method, path).then().statusCode(expected);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/user/user-controller/update-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Update profile - rejects an invalid request")
    void updateValidatesRequest(String body, int expected, String reason)
    {
        sendAsOwner("PUT", "", body, expected);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/user/user-controller/change-email-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Change email - rejects an invalid request")
    void changeEmailValidatesRequest(String body, int expected, String reason)
    {
        sendAsOwner("PATCH", "/email", body, expected);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/user/user-controller/change-password-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Change password - rejects an invalid request")
    void changePasswordValidatesRequest(String body, int expected, String reason)
    {
        sendAsOwner("PATCH", "/password", body, expected);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/user/user-controller/change-role-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Change role - rejects an invalid request")
    void changeRoleValidatesRequest(String body, int expected, String reason)
    {
        sendAsOwner("PATCH", "/role", body, expected);
    }

    private void sendAsOwner(String method, String pathSuffix, String body, int expected)
    {
        RegisteredUser owner = register();
        RequestSpecification request = givenToken(owner.token());
        if (body != null)
        {
            request.body(body);
        }

        request.when().request(method, "/users/" + owner.id() + pathSuffix).then().statusCode(expected);
    }

    private String validBody(String method, String path)
    {
        if (method.equals("PUT"))
        {
            return """
                    { "firstName": "Ada", "lastName": "Byron" }
                    """;
        }
        if (path.endsWith("/email"))
        {
            return """
                    { "email": "changed.%d@example.com" }
                    """.formatted(EMAIL_COUNTER.incrementAndGet());
        }
        if (path.endsWith("/password"))
        {
            return """
                    { "currentPassword": "%s", "newPassword": "NewPassw0rd!" }
                    """.formatted(PASSWORD);
        }
        if (path.endsWith("/role"))
        {
            return """
                    { "role": "PROJECT_MANAGER" }
                    """;
        }
        return null;
    }

    private RegisteredUser register()
    {
        String email = "user.%d@example.com".formatted(EMAIL_COUNTER.incrementAndGet());
        String response = givenToken(null).body("""
                        { "firstName": "Test", "lastName": "User", "email": "%s", "password": "%s" }
                        """.formatted(email, PASSWORD))
                .when().post("/auth/register")
                .then().statusCode(201)
                .extract().asString();

        long id = readJson(response).get("id").asLong();
        return new RegisteredUser(id, JWTUtil.createToken(id, email, Role.EMPLOYEE));
    }

    private record RegisteredUser(long id, String token)
    {
    }
}
