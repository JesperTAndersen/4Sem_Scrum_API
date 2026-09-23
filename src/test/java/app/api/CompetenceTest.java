package app.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.response.ResponseBodyExtractionOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApiTest.class)
class CompetenceTest
{
    @Test
    void createValidateUpdateActiveStateAndTimestamps() throws Exception
    {
        JsonNode created = createCompetence("Carpenter", 850.00);
        long carpenterId = created.get("id").asLong();

        assertTrue(created.get("active").asBoolean());
        assertTrue(created.hasNonNull("createdAt"));
        assertTrue(created.hasNonNull("updatedAt"));

        createCompetence("Electrician", 900.00);

        givenAuthenticated()
                .body("{\"name\":\"C\",\"rate\":850.00}")
                .when()
                .post("/competences")
                .then()
                .statusCode(400);

        givenAuthenticated()
                .body("{\"name\":\"carpenter\",\"rate\":950.00}")
                .when()
                .post("/competences")
                .then()
                .statusCode(400);

        givenAuthenticated()
                .body("{\"name\":\"Electrician\",\"rate\":850.00}")
                .when()
                .put("/competences/" + carpenterId)
                .then()
                .statusCode(400);

        givenAuthenticated()
                .body("{\"name\":\"Carpenter\",\"rate\":0}")
                .when()
                .put("/competences/" + carpenterId)
                .then()
                .statusCode(400);

        givenAuthenticated()
                .when()
                .patch("/competences/" + carpenterId + "/deactivate")
                .then()
                .statusCode(204);
        assertFalse(getCompetence(carpenterId).get("active").asBoolean());

        givenAuthenticated()
                .when()
                .patch("/competences/" + carpenterId + "/activate")
                .then()
                .statusCode(204);
        assertTrue(getCompetence(carpenterId).get("active").asBoolean());

        givenAuthenticated()
                .when()
                .delete("/competences/" + carpenterId)
                .then()
                .statusCode(204);
        givenAuthenticated()
                .when()
                .get("/competences/" + carpenterId)
                .then()
                .statusCode(404);

        givenAuthenticated()
                .when()
                .patch("/competences/999999/activate")
                .then()
                .statusCode(404);

        givenAuthenticated()
                .when()
                .patch("/competences/0/activate")
                .then()
                .statusCode(400);
    }

    private JsonNode createCompetence(String name, double rate) throws Exception
    {
        ResponseBodyExtractionOptions response = givenAuthenticated()
                .body("{\"name\":\"%s\",\"rate\":%s}".formatted(name, rate))
                .when()
                .post("/competences")
                .then()
                .statusCode(201)
                .extract()
                .body();

        return ApiTest.objectMapper.readTree(response.asString());
    }

    private JsonNode getCompetence(long id) throws Exception
    {
        ResponseBodyExtractionOptions response = givenAuthenticated()
                .when()
                .get("/competences/" + id)
                .then()
                .statusCode(200)
                .extract()
                .body();

        return ApiTest.objectMapper.readTree(response.asString());
    }

    private io.restassured.specification.RequestSpecification givenAuthenticated()
    {
        return given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + ApiTest.JWT_TOKEN);
    }
}