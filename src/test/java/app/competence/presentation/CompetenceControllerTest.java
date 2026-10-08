package app.competence.presentation;

import static app.config.ApiTestExtension.UNKNOWN_ID;
import static app.config.ApiTestExtension.createAsManager;
import static app.config.ApiTestExtension.givenManager;
import static app.config.ApiTestExtension.givenToken;
import static app.config.ApiTestExtension.tokenFor;

import app.config.ApiTestExtension;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(ApiTestExtension.class)
class CompetenceControllerTest
{
    // Competence names are unique, so every created competence gets its own name.
    private static final AtomicInteger NAME_COUNTER = new AtomicInteger();

    @ParameterizedTest(name = "[{index}] {5}")
    @CsvFileSource(resources = "/testcases/competence/competence-controller/endpoint-access.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("All endpoints - require an authenticated user and an existing competence")
    void enforcesEndpointAccess(String method, String path, String target, String token, int expected, String reason)
    {
        RequestSpecification request = givenToken(tokenFor(token));
        if (method.equals("POST") || method.equals("PUT"))
        {
            request.body(validBody());
        }

        request.when().request(method, path.replace("{id}", targetId(target)))
                .then().statusCode(expected);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/competence/competence-controller/path-id.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Endpoints with an id - reject an invalid path id")
    void rejectsInvalidPathId(String method, String path, int expected, String reason)
    {
        RequestSpecification request = givenManager();
        if (method.equals("PUT"))
        {
            request.body(validBody());
        }

        request.when().request(method, path).then().statusCode(expected);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/competence/competence-controller/create-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Create - rejects an invalid request")
    void createValidatesRequest(String body, int expected, String reason)
    {
        RequestSpecification request = givenManager();
        if (body != null)
        {
            request.body(body);
        }

        request.when().post("/competences").then().statusCode(expected);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/competence/competence-controller/update-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Update - rejects an invalid request")
    void updateValidatesRequest(String body, int expected, String reason)
    {
        long id = createAsManager("/competences", validBody());
        RequestSpecification request = givenManager();
        if (body != null)
        {
            request.body(body);
        }

        request.when().put("/competences/" + id).then().statusCode(expected);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/competence/competence-controller/delete-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Delete - rejects deleting a competence that a task uses")
    void deleteValidatesRequest(boolean usedByTask, int expected, String reason)
    {
        long id = createAsManager("/competences", validBody());
        if (usedByTask)
        {
            long stageId = createAsManager("/stages", """
                    { "projectId": 1, "name": "Competence usage stage" }
                    """);
            createAsManager("/tasks", """
                    { "stageId": %d, "name": "Uses competence", "competenceId": %d, "estimate": 8.0, "minimumDurationInDays": 0 }
                    """.formatted(stageId, id));
        }

        givenManager().when().delete("/competences/" + id).then().statusCode(expected);
    }

    private String validBody()
    {
        return """
                { "name": "endpoint competence %d", "rate": 850.00 }
                """.formatted(NAME_COUNTER.incrementAndGet());
    }

    private String targetId(String target)
    {
        return switch (target)
        {
            case "EXISTING" -> String.valueOf(createAsManager("/competences", validBody()));
            case "UNKNOWN" -> String.valueOf(UNKNOWN_ID);
            default -> "";
        };
    }
}
