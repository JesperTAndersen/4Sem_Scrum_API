package app.api;

import static io.restassured.RestAssured.given;

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
            .get("/api/v1/projects")
            .then()
            .statusCode(401);
    }
    @Test
    void get()
    {
        given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .when()
            .get("/api/v1/projects")
            .then()
            .statusCode(200);
    }

    @Test
    void create()
    {
        String JSON = """
        {
            "title" : "New Project",
            "description" : "A new Project",
            "startDate" : "2030-01-01",
            "deadline" : "2030-04-01"
        }
        """;

        given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer "+ApiTest.JWT_TOKEN)
            .body(JSON)
            .when()
            .post("/api/v1/projects")
            .then()
            .statusCode(201);
    }
}
