package app.api;

import app.security.domain.Role;
import app.utils.JWTUtil;
import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.response.ResponseBodyExtractionOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ApiTest.class)
class TaskTest
{
    @Test
    void scheduledDurationIsTheGreaterOfLaborAndMinimumDuration() throws Exception
    {
        long taskId = createTask("Cannot be accelerated", 3);
        long competenceId = createCompetence("Planning");

        JsonNode assigned = update(taskId, """
                { "competenceId": %d, "estimate": 7.5 }
                """.formatted(competenceId));
        assertEquals(1.0, assigned.get("laborDurationInDays").asDouble());
        assertEquals(3.0, assigned.get("scheduledDurationInDays").asDouble());

        JsonNode recalculated = update(taskId, """
                { "competenceId": %d, "estimate": 3.75 }
                """.formatted(competenceId));
        assertEquals(0.5, recalculated.get("laborDurationInDays").asDouble());
        assertEquals(3.0, recalculated.get("scheduledDurationInDays").asDouble());
    }

    @Test
    void taskHasOnlyOneCompetenceAndItCanBeRemoved() throws Exception
    {
        long taskId = createTask("Single competence task", 1);
        long competenceId = createCompetence("Carpentry");

        JsonNode assigned = update(taskId, """
                { "competenceId": %d, "estimate": 15.0 }
                """.formatted(competenceId));
        assertEquals(competenceId, assigned.get("competenceId").asLong());
        assertEquals(15.0, assigned.get("estimate").asDouble());
        assertEquals(2.0, assigned.get("scheduledDurationInDays").asDouble());

        JsonNode removed = update(taskId, "{ \"competenceId\": 0 }");
        assertEquals(0, removed.get("competenceId").asLong());
        assertEquals(0.0, removed.get("estimate").asDouble());
        assertEquals(1.0, removed.get("scheduledDurationInDays").asDouble());
    }

    @Test
    void negativeMinimumDurationIsRejected() throws Exception
    {
        long stageId = createStage("Invalid duration stage");
        long competenceId = createCompetence("Invalid duration competence");
        givenAuthenticated().body("""
                { "stageId": %d, "name": "Invalid", "competenceId": %d, "minimumDurationInDays": -1 }
                """.formatted(stageId, competenceId))
                .when().post("/tasks").then().statusCode(400);
    }

    @Test
    void missingEstimateIsRejectedWhenCreatingATask() throws Exception
    {
        long stageId = createStage("Missing estimate stage");
        long competenceId = createCompetence("Missing estimate competence");
        givenAuthenticated().body("""
                { "stageId": %d, "name": "Missing estimate", "competenceId": %d, "minimumDurationInDays": 1 }
                """.formatted(stageId, competenceId))
                .when().post("/tasks").then().statusCode(400);
    }

    @Test
    void getRequiresProjectManager()
    {
        given().header("Content-Type", "application/json").when().get("/tasks").then().statusCode(401);
        String employeeToken = JWTUtil.createToken(2L, "employee@example.org", Role.EMPLOYEE);
        given().header("Authorization", "Bearer " + employeeToken).when().get("/tasks").then().statusCode(403);
    }

    private long createTask(String name, int minimumDurationInDays) throws Exception
    {
        long stageId = createStage(name + " stage");
        long competenceId = createCompetence(name + " competence");
        ResponseBodyExtractionOptions response = givenAuthenticated().body("""
                { "stageId": %d, "name": "%s", "competenceId": %d, "estimate": 0.0, "minimumDurationInDays": %d }
                """.formatted(stageId, name, competenceId, minimumDurationInDays))
                .when().post("/tasks").then().statusCode(201).extract().body();
        return ApiTest.objectMapper.readTree(response.asString()).get("id").asLong();
    }

    private JsonNode update(long taskId, String json) throws Exception
    {
        ResponseBodyExtractionOptions response = givenAuthenticated().body(json)
                .when().put("/tasks/" + taskId).then().statusCode(200).extract().body();
        return ApiTest.objectMapper.readTree(response.asString());
    }

    private long createStage(String name) throws Exception
    {
        ResponseBodyExtractionOptions response = givenAuthenticated().body("""
                { "projectId": 1, "name": "%s" }
                """.formatted(name))
                .when().post("/stages").then().statusCode(201).extract().body();
        return ApiTest.objectMapper.readTree(response.asString()).get("id").asLong();
    }

    private long createCompetence(String name) throws Exception
    {
        ResponseBodyExtractionOptions response = givenAuthenticated().body("""
                { "name": "%s", "rate": 850.00 }
                """.formatted(name))
                .when().post("/competences").then().statusCode(201).extract().body();
        return ApiTest.objectMapper.readTree(response.asString()).get("id").asLong();
    }

    private io.restassured.specification.RequestSpecification givenAuthenticated()
    {
        return given().header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + ApiTest.JWT_TOKEN);
    }
}
