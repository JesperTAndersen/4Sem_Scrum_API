package app.project.presentation;

import static app.config.ApiTestExtension.UNKNOWN_ID;
import static app.config.ApiTestExtension.createAsManager;
import static app.config.ApiTestExtension.givenManager;
import static app.config.ApiTestExtension.givenToken;
import static app.config.ApiTestExtension.readJson;
import static app.config.ApiTestExtension.tokenFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
class ProjectControllerTest
{
    private static final String VALID_CREATE_BODY = """
            { "title": "New Project", "description": "A new Project", "startDate": "2030-01-01", "deadline": "2030-04-01" }
            """;
    private static final String VALID_UPDATE_BODY = """
            { "title": "Updated Project", "description": "An updated Project", "startDate": "2030-02-01",
              "deadline": "2030-06-01", "status": "PLANNED" }
            """;

    @ParameterizedTest(name = "[{index}] {5}")
    @CsvFileSource(resources = "/testcases/project/project-controller/endpoint-access.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("All endpoints - require a PROJECT_MANAGER token and an existing project")
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
    @CsvFileSource(resources = "/testcases/project/project-controller/path-id.csv", numLinesToSkip = 1, nullValues = "NULL")
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
    @CsvFileSource(resources = "/testcases/project/project-controller/create-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Create - rejects an invalid request")
    void createValidatesRequest(String body, int expected, String reason)
    {
        RequestSpecification request = givenManager();
        if (body != null)
        {
            request.body(body);
        }

        request.when().post("/projects").then().statusCode(expected);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/project/project-controller/update-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Update - rejects an invalid request")
    void updateValidatesRequest(String body, int expected, String reason)
    {
        long id = createAsManager("/projects", VALID_CREATE_BODY);
        RequestSpecification request = givenManager();
        if (body != null)
        {
            request.body(body);
        }

        request.when().put("/projects/" + id).then().statusCode(expected);
    }

    @Test
    @DisplayName("Get all - should return project summaries with task counts")
    void getAllReturnsProjectSummaries()
    {
        String response = givenManager()
                .when().get("/projects")
                .then().statusCode(200)
                .extract().asString();

        JsonNode projects = readJson(response);
        assertTrue(projects.isArray());
        assertTrue(projects.size() >= 3);
        assertTrue(projects.get(0).has("id"));
        assertTrue(projects.get(0).has("title"));
        assertTrue(projects.get(0).has("taskCountDTO"));
        assertTrue(projects.get(0).has("status"));
    }

    @Test
    @DisplayName("Create and get - should return the created project as a DRAFT with audit users and stages")
    void createAndGet()
    {
        long id = createAsManager("/projects", VALID_CREATE_BODY);

        String response = givenManager()
                .when().get("/projects/" + id)
                .then().statusCode(200)
                .extract().asString();

        JsonNode project = readJson(response);
        assertEquals(id, project.get("id").asLong());
        assertEquals("New Project", project.get("title").asText());
        assertEquals("A new Project", project.get("description").asText());
        assertEquals("2030-01-01", project.get("startDate").asText());
        assertEquals("2030-04-01", project.get("deadline").asText());
        assertEquals("DRAFT", project.get("status").asText());
        assertTrue(project.get("createdBy").has("id"));
        assertTrue(project.get("updatedBy").has("id"));
        assertTrue(project.get("stages").isArray());
    }

    @Test
    @DisplayName("Update - should return the updated project fields")
    void update()
    {
        long id = createAsManager("/projects", VALID_CREATE_BODY);

        String response = givenManager().body(VALID_UPDATE_BODY)
                .when().put("/projects/" + id)
                .then().statusCode(200)
                .extract().asString();

        JsonNode project = readJson(response);
        assertEquals(id, project.get("id").asLong());
        assertEquals("Updated Project", project.get("title").asText());
        assertEquals("An updated Project", project.get("description").asText());
        assertEquals("PLANNED", project.get("status").asText());
        assertEquals("2030-02-01", project.get("startDate").asText());
        assertEquals("2030-06-01", project.get("deadline").asText());
    }

    @Test
    @DisplayName("Delete - should make the project unavailable")
    void delete()
    {
        long id = createAsManager("/projects", VALID_CREATE_BODY);

        givenManager().when().delete("/projects/" + id).then().statusCode(204);

        givenManager().when().get("/projects/" + id).then().statusCode(404);
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
            case "EXISTING" -> String.valueOf(createAsManager("/projects", VALID_CREATE_BODY));
            case "UNKNOWN" -> String.valueOf(UNKNOWN_ID);
            default -> "";
        };
    }
}
