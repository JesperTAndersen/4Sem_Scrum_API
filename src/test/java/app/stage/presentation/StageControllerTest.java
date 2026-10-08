package app.stage.presentation;

import static app.config.ApiTestExtension.UNKNOWN_ID;
import static app.config.ApiTestExtension.createAsManager;
import static app.config.ApiTestExtension.givenManager;
import static app.config.ApiTestExtension.givenToken;
import static app.config.ApiTestExtension.readJson;
import static app.config.ApiTestExtension.tokenFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.config.ApiTestExtension;
import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

@ExtendWith(ApiTestExtension.class)
class StageControllerTest
{
    private static final String VALID_CREATE_BODY = """
            { "projectId": 1, "name": "Planning" }
            """;
    private static final String VALID_UPDATE_BODY = """
            { "name": "Implementation" }
            """;

    @ParameterizedTest(name = "[{index}] {5}")
    @CsvFileSource(resources = "/testcases/stage/stage-controller/endpoint-access.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("All endpoints - require a PROJECT_MANAGER token and an existing stage")
    void enforcesEndpointAccess(String method, String path, String target, String token, int expected, String reason)
    {
        RequestSpecification request = givenToken(tokenFor(token));
        String body = validBody(method);
        if (body != null)
        {
            request.body(body);
        }

        request.when().request(method, path.replace("{id}", targetId(target)))
                .then().statusCode(expected);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/stage/stage-controller/path-id.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Endpoints with an id - reject an invalid path id")
    void rejectsInvalidPathId(String method, String path, int expected, String reason)
    {
        RequestSpecification request = givenManager();
        String body = validBody(method);
        if (body != null)
        {
            request.body(body);
        }

        request.when().request(method, path).then().statusCode(expected);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/stage/stage-controller/create-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Create - rejects an invalid request")
    void createValidatesRequest(String body, int expected, String reason)
    {
        RequestSpecification request = givenManager();
        if (body != null)
        {
            request.body(body);
        }

        request.when().post("/stages").then().statusCode(expected);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/stage/stage-controller/update-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Update - rejects an invalid request")
    void updateValidatesRequest(String body, int expected, String reason)
    {
        long id = createAsManager("/stages", VALID_CREATE_BODY);
        RequestSpecification request = givenManager();
        if (body != null)
        {
            request.body(body);
        }

        request.when().put("/stages/" + id).then().statusCode(expected);
    }

    @Test
    @DisplayName("Create, update and get - should return the trimmed stage and show it in its project")
    void createUpdateAndGet()
    {
        String createdResponse = givenManager().body("""
                        { "projectId": 1, "name": " Discovery " }
                        """)
                .when().post("/stages")
                .then().statusCode(201)
                .extract().asString();

        JsonNode createdStage = readJson(createdResponse);
        assertTrue(createdStage.has("id"));
        assertEquals("Discovery", createdStage.get("name").asText());
        assertTrue(createdStage.get("tasks").isArray());
        assertEquals(0, createdStage.get("tasks").size());
        long id = createdStage.get("id").asLong();

        String updatedResponse = givenManager().body("""
                        { "name": "Planning" }
                        """)
                .when().put("/stages/" + id)
                .then().statusCode(200)
                .extract().asString();

        JsonNode updatedStage = readJson(updatedResponse);
        assertEquals(id, updatedStage.get("id").asLong());
        assertEquals("Planning", updatedStage.get("name").asText());
        assertTrue(updatedStage.get("tasks").isArray());

        String stageResponse = givenManager()
                .when().get("/stages/" + id)
                .then().statusCode(200)
                .extract().asString();

        JsonNode fetchedStage = readJson(stageResponse);
        assertEquals(id, fetchedStage.get("id").asLong());
        assertEquals("Planning", fetchedStage.get("name").asText());
        assertTrue(fetchedStage.get("tasks").isArray());

        String projectResponse = givenManager()
                .when().get("/projects/1")
                .then().statusCode(200)
                .extract().asString();

        JsonNode stageInProject = null;
        for (JsonNode stage : readJson(projectResponse).get("stages"))
        {
            if (stage.get("id").asLong() == id)
            {
                stageInProject = stage;
            }
        }

        assertNotNull(stageInProject);
        assertEquals("Planning", stageInProject.get("name").asText());
        assertTrue(stageInProject.get("tasks").isArray());
    }

    private String validBody(String method)
    {
        return switch (method)
        {
            case "POST" -> VALID_CREATE_BODY;
            case "PUT" -> VALID_UPDATE_BODY;
            default -> null;
        };
    }

    private String targetId(String target)
    {
        return switch (target)
        {
            case "EXISTING" -> String.valueOf(createAsManager("/stages", VALID_CREATE_BODY));
            case "UNKNOWN" -> String.valueOf(UNKNOWN_ID);
            default -> "";
        };
    }
}
