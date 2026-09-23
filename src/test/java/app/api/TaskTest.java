package app.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.security.domain.Role;
import app.utils.JWTUtil;
import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.response.ResponseBodyExtractionOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ApiTest.class)
class TaskTest
{
    @Test
    void get401()
    {
        given()
            .header("Content-Type", "application/json")
            .when()
            .get("/tasks")
            .then()
            .statusCode(401);
    }

    @Test
    void createUpdateAndGet() throws Exception
    {
        Long stageId = createStage("Task test stage");
        Long backendCompetenceId = createCompetence("Backend development");
        Long testingCompetenceId = createCompetence("Testing");

        String JSON = """
        {
            "stageId": %d,
            "name": "Define requirements",
            "minDuration": 8.0
        }
        """.formatted(stageId);

        ResponseBodyExtractionOptions taskResponse = given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .body(JSON)
            .when()
            .post("/tasks")
            .then()
            .statusCode(201)
            .extract().body();

        JsonNode createdTask = ApiTest.objectMapper.readTree(taskResponse.asString());
        long taskId = createdTask.get("id").asLong();
        assertEquals("Define requirements", createdTask.get("name").asText());
        assertEquals("NOT_STARTED", createdTask.get("status").asText());

        String updateJSON = """
        {
            "name": "Updated requirements",
            "minDuration": 16.0
        }
        """.formatted();

        ResponseBodyExtractionOptions updatedResponse = given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .body(updateJSON)
            .when()
            .put("/tasks/"+taskId)
            .then()
            .statusCode(200)
            .extract().body();

        JsonNode updatedTask = ApiTest.objectMapper.readTree(updatedResponse.asString());
        assertEquals(taskId, updatedTask.get("id").asLong());
        assertEquals("Updated requirements", updatedTask.get("name").asText());
        assertEquals(16.0, updatedTask.get("minDuration").asDouble());

        String competenceJSON = """
        {
            "competenceId": %d,
            "estimate": 32.0
        }
        """.formatted(backendCompetenceId);
        given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .body(competenceJSON)
            .when()
            .put("/tasks/"+taskId)
            .then()
            .statusCode(200);

        JsonNode fetchedTask = getJson("/tasks/" + taskId);
        assertEquals("Updated requirements", fetchedTask.get("name").asText());
        assertEquals(16.0, fetchedTask.get("minDuration").asDouble());
        assertEquals(backendCompetenceId, fetchedTask.get("competenceId").asLong());
        assertEquals(32.0, fetchedTask.get("estimate").asDouble());

        JsonNode stage = getJson("/stages/" + stageId);
        JsonNode taskInStage = findById(stage.get("tasks"), taskId);
        assertNotNull(taskInStage, "The created task must be returned by its stage");
        assertEquals("Updated requirements", taskInStage.get("name").asText());

        JsonNode project = getJson("/projects/1");
        JsonNode stageInProject = findById(project.get("stages"), stageId);
        assertNotNull(stageInProject, "The created stage must be returned by its project");
        JsonNode taskInProject = findById(stageInProject.get("tasks"), taskId);
        assertNotNull(taskInProject,
            "The task must be returned in the Project -> Stage -> Task hierarchy");

        String removeCompetenceJSON = """
        {
            "competenceId": %d
        }
        """.formatted(0);
        given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .body(removeCompetenceJSON)
            .when()
            .put("/tasks/"+taskId)
            .then()
            .statusCode(200);
        fetchedTask = getJson("/tasks/" + taskId);
        assertEquals("Updated requirements", fetchedTask.get("name").asText());
        assertEquals(16.0, fetchedTask.get("minDuration").asDouble());
        assertEquals(0, fetchedTask.get("competenceId").asLong());
        assertEquals(0.0, fetchedTask.get("estimate").asDouble());
    }

    @Test
    void get404()
    {
        given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .when()
            .get("/tasks/100")
            .then()
            .statusCode(404);
    }

    @Test
    void employeeGets403()
    {
        String employeeToken = JWTUtil.createToken(2L, "employee@example.org", Role.EMPLOYEE);

        given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+employeeToken)
            .when()
            .get("/tasks")
            .then()
            .statusCode(403);
    }

    private Long createStage(String name) throws Exception
    {
        String JSON = """
        {
            "projectId": 1,
            "name": "%s"
        }
        """.formatted(name);

        ResponseBodyExtractionOptions response = given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .body(JSON)
            .when()
            .post("/stages")
            .then()
            .statusCode(201)
            .extract().body();

        return ApiTest.objectMapper.readTree(response.asString()).get("id").asLong();
    }

    private Long createCompetence(String name) throws Exception
    {
        String JSON = """
        {
            "name": "%s",
            "rate": 850.00
        }
        """.formatted(name);

        ResponseBodyExtractionOptions response = given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .body(JSON)
            .when()
            .post("/competences")
            .then()
            .statusCode(201)
            .extract().body();

        return ApiTest.objectMapper.readTree(response.asString()).get("id").asLong();
    }

    private JsonNode getJson(String path) throws Exception
    {
        ResponseBodyExtractionOptions response = given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .when()
            .get(path)
            .then()
            .statusCode(200)
            .extract().body();

        return ApiTest.objectMapper.readTree(response.asString());
    }

    private JsonNode findById(JsonNode values, long id)
    {
        assertTrue(values.isArray());
        for (JsonNode value : values)
        {
            if (value.get("id").asLong() == id)
            {
                return value;
            }
        }
        return null;
    }
}