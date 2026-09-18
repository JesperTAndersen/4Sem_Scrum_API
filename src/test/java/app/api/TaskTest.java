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
            "estimate": 8.0,
            "competenceIds": [%d, %d]
        }
        """.formatted(stageId, backendCompetenceId, testingCompetenceId);

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
        assertCompetences(createdTask, backendCompetenceId, testingCompetenceId);

        String updateJSON = """
        {
            "name": "Updated requirements",
            "estimate": 16.0,
            "competenceIds": [%d, %d]
        }
        """.formatted(backendCompetenceId, testingCompetenceId);

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
        assertEquals(16.0, updatedTask.get("estimate").asDouble());
        assertCompetences(updatedTask, backendCompetenceId, testingCompetenceId);

        JsonNode fetchedTask = getJson("/tasks/" + taskId);
        assertEquals("Updated requirements", fetchedTask.get("name").asText());
        assertEquals(16.0, fetchedTask.get("estimate").asDouble());
        assertCompetences(fetchedTask, backendCompetenceId, testingCompetenceId);

        JsonNode stage = getJson("/stages/" + stageId);
        JsonNode taskInStage = findById(stage.get("tasks"), taskId);
        assertNotNull(taskInStage, "The created task must be returned by its stage");
        assertEquals("Updated requirements", taskInStage.get("name").asText());
        assertCompetences(taskInStage, backendCompetenceId, testingCompetenceId);

        JsonNode project = getJson("/projects/1");
        JsonNode stageInProject = findById(project.get("stages"), stageId);
        assertNotNull(stageInProject, "The created stage must be returned by its project");
        JsonNode taskInProject = findById(stageInProject.get("tasks"), taskId);
        assertNotNull(taskInProject,
            "The task must be returned in the Project -> Stage -> Task hierarchy");
        assertCompetences(taskInProject, backendCompetenceId, testingCompetenceId);
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

    private void assertCompetences(JsonNode task, long firstId, long secondId)
    {
        JsonNode competences = task.get("competences");
        assertTrue(competences.isArray());
        assertEquals(2, competences.size());
        assertNotNull(findById(competences, firstId));
        assertNotNull(findById(competences, secondId));
    }
}
