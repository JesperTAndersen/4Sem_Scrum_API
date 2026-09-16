package app.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.databind.JsonNode;

import io.restassured.response.ResponseBodyExtractionOptions;

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
        given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .when()
            .get("/projects")
            .then()
            .statusCode(200);
    }

    @Test
    void create() throws Exception
    {
        String JSON = """
        {
            "title" : "New Project",
            "description" : "A new Project",
            "startDate" : "2030-01-01",
            "deadline" : "2030-04-01"
        }
        """;

        ResponseBodyExtractionOptions actual = given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .body(JSON)
            .when()
            .post("/projects")
            .then()
            .statusCode(201)
            .extract().body();

        JsonNode node = ApiTest.objectMapper.readTree(actual.asString());
        assertTrue(node.has("id"));
        Long id = node.get("id").asLong();

        given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .when()
            .get("/projects/"+id)
            .then()
            .statusCode(200);
    }
}
