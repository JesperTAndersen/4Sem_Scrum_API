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
class ProjectTest
{
    @Test
    void get401()
    {
        given()
            .header("Content-Type", "application/json")
            .when()
            .get("/projects")
            .then()
            .statusCode(401);
    }

    @Test
    void employeeGets403()
    {
        String employeeToken = JWTUtil.createToken(2L, "employee@example.org", Role.EMPLOYEE);

        given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+employeeToken)
            .when()
            .get("/projects")
            .then()
            .statusCode(403);
    }

    @Test
    void get404()
    {
        given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .when()
            .get("/projects/100")
            .then()
            .statusCode(404);
    }

    @Test
    void get()
    {
        ResponseBodyExtractionOptions response = given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .when()
            .get("/projects")
            .then()
            .statusCode(200)
            .extract().body();

        try
        {
            JsonNode projects = ApiTest.objectMapper.readTree(response.asString());
            assertTrue(projects.isArray());
            assertTrue(projects.size() >= 3);
            assertTrue(projects.get(0).has("id"));
            assertTrue(projects.get(0).has("title"));
            assertTrue(projects.get(0).has("taskCountDTO"));
            assertTrue(projects.get(0).has("status"));
        }
        catch (Exception e)
        {
            throw new AssertionError("Project list response was not valid JSON", e);
        }
    }

    @Test
    void createAndGet() throws Exception
    {
        Long id = createProject("New Project");

        ResponseBodyExtractionOptions response = given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .when()
            .get("/projects/"+id)
            .then()
            .statusCode(200)
            .extract().body();

        JsonNode project = ApiTest.objectMapper.readTree(response.asString());
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
    void update() throws Exception
    {
        Long id = createProject("Project to update");
        String JSON = """
        {
            "title" : "Updated Project",
            "description" : "An updated Project",
            "startDate" : "2030-02-01",
            "deadline" : "2030-06-01",
            "status" : "PLANNED"
        }
        """;

        ResponseBodyExtractionOptions response = given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .body(JSON)
            .when()
            .put("/projects/"+id)
            .then()
            .statusCode(200)
            .extract().body();

        JsonNode project = ApiTest.objectMapper.readTree(response.asString());
        assertEquals(id, project.get("id").asLong());
        assertEquals("Updated Project", project.get("title").asText());
        assertEquals("An updated Project", project.get("description").asText());
        assertEquals("PLANNED", project.get("status").asText());
        assertEquals("2030-02-01", project.get("startDate").asText());
        assertEquals("2030-06-01", project.get("deadline").asText());
    }

    @Test
    void createRejectsInvalidDateRange()
    {
        String JSON = """
        {
            "title" : "Invalid Project",
            "description" : "The deadline is before the start date",
            "startDate" : "2030-06-01",
            "deadline" : "2030-01-01"
        }
        """;

        given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .body(JSON)
            .when()
            .post("/projects")
            .then()
            .statusCode(400);
    }

    @Test
    void delete() throws Exception
    {
        Long id = createProject("Project to delete");

        given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .when()
            .delete("/projects/"+id)
            .then()
            .statusCode(204);

        given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .when()
            .get("/projects/"+id)
            .then()
            .statusCode(404);
    }

    private Long createProject(String title) throws Exception
    {
        String JSON = """
        {
            "title" : "%s",
            "description" : "A new Project",
            "startDate" : "2030-01-01",
            "deadline" : "2030-04-01"
        }
        """.formatted(title);

        ResponseBodyExtractionOptions response = given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .body(JSON)
            .when()
            .post("/projects")
            .then()
            .statusCode(201)
            .extract().body();

        JsonNode project = ApiTest.objectMapper.readTree(response.asString());
        assertTrue(project.has("id"));
        return project.get("id").asLong();
    }
}
