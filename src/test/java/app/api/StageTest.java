package app.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.security.domain.Role;
import app.utils.JWTUtil;
import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.response.ResponseBodyExtractionOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ApiTest.class)
class StageTest
{
    @Test
    void get401()
    {
        given()
            .header("Content-Type", "application/json")
            .when()
            .get("/stages")
            .then()
            .statusCode(401);
    }

    @Test
    void createUpdateAndGet() throws Exception
    {
        String createJSON = """
        {
            "projectId" : 1,
            "name" : " Discovery "
        }
        """;

        ResponseBodyExtractionOptions createdResponse = given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .body(createJSON)
            .when()
            .post("/stages")
            .then()
            .statusCode(201)
            .extract().body();

        JsonNode createdStage = ApiTest.objectMapper.readTree(createdResponse.asString());
        assertTrue(createdStage.has("id"));
        assertEquals("Discovery", createdStage.get("name").asText());
        assertTrue(createdStage.has("tasks"));
        assertTrue(createdStage.get("tasks").isArray());
        assertEquals(0, createdStage.get("tasks").size());
        Long id = createdStage.get("id").asLong();

        String updateJSON = """
        {
            "name" : "Planning"
        }
        """;

        ResponseBodyExtractionOptions updatedResponse = given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .body(updateJSON)
            .when()
            .put("/stages/"+id)
            .then()
            .statusCode(200)
            .extract().body();

        JsonNode updatedStage = ApiTest.objectMapper.readTree(updatedResponse.asString());
        assertEquals(id, updatedStage.get("id").asLong());
        assertEquals("Planning", updatedStage.get("name").asText());
        assertTrue(updatedStage.get("tasks").isArray());

        ResponseBodyExtractionOptions stageResponse = given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .when()
            .get("/stages/"+id)
            .then()
            .statusCode(200)
            .extract().body();

        JsonNode fetchedStage = ApiTest.objectMapper.readTree(stageResponse.asString());
        assertEquals(id, fetchedStage.get("id").asLong());
        assertEquals("Planning", fetchedStage.get("name").asText());
        assertTrue(fetchedStage.get("tasks").isArray());

        ResponseBodyExtractionOptions projectResponse = given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .when()
            .get("/projects/1")
            .then()
            .statusCode(200)
            .extract().body();

        JsonNode project = ApiTest.objectMapper.readTree(projectResponse.asString());
        assertTrue(project.get("stages").isArray());

        JsonNode stageInProject = null;
        for (JsonNode stage : project.get("stages"))
        {
            if (stage.get("id").asLong() == id)
            {
                stageInProject = stage;
                break;
            }
        }

        assertTrue(stageInProject != null);
        assertEquals("Planning", stageInProject.get("name").asText());
        assertTrue(stageInProject.get("tasks").isArray());
    }

    @Test
    void get404()
    {
        given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .when()
            .get("/stages/100")
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
            .get("/stages")
            .then()
            .statusCode(403);
    }
}
