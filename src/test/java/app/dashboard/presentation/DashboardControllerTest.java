package app.dashboard.presentation;

import static app.config.ApiTestExtension.createAsManager;
import static app.config.ApiTestExtension.givenManager;
import static app.config.ApiTestExtension.givenToken;
import static app.config.ApiTestExtension.readJson;
import static app.config.ApiTestExtension.tokenFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.config.ApiTestExtension;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(ApiTestExtension.class)
class DashboardControllerTest
{
    // Competence names are unique, so every created competence gets its own name.
    private static final AtomicInteger NAME_COUNTER = new AtomicInteger();

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/dashboard/dashboard-controller/get-summary-access.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Get summary - requires a PROJECT_MANAGER token")
    void getSummaryRequiresProjectManager(String token, int expected, String reason)
    {
        givenToken(tokenFor(token))
                .when().get("/dashboard/summary")
                .then().statusCode(expected);
    }

    @Test
    @DisplayName("Get summary - should match the totals and schedules returned by the project endpoints")
    void getSummaryMatchesProjectEndpoints()
    {
        long projectId = createInfeasibleProjectWithTask();

        JsonNode summary = getJson("/dashboard/summary");
        JsonNode projects = getJson("/projects");

        double expectedHours = 0;
        BigDecimal expectedCost = BigDecimal.ZERO;
        Map<String, Integer> expectedByStatus = new HashMap<>();
        for (JsonNode slimProject : projects)
        {
            JsonNode project = getJson("/projects/" + slimProject.get("id").asLong());
            expectedHours += project.get("totalEstimatedHours").asDouble();
            expectedCost = expectedCost.add(project.get("totalCost").decimalValue());
            String status = project.get("status").asText();
            expectedByStatus.put(status, expectedByStatus.getOrDefault(status, 0) + 1);
        }

        assertEquals(projects.size(), summary.at("/projects/total").asInt());
        for (String status : List.of("DRAFT", "PLANNED", "IN_PROGRESS", "COMPLETED"))
        {
            assertEquals(expectedByStatus.getOrDefault(status, 0), summary.at("/projects/byStatus/" + status).asInt());
        }
        assertEquals(expectedHours, summary.get("totalEstimatedHours").asDouble(), 0.0001);
        assertEquals(0, expectedCost.compareTo(summary.get("totalCost").decimalValue()));

        for (JsonNode upcoming : summary.get("upcomingDeadlines"))
        {
            JsonNode project = getJson("/projects/" + upcoming.get("projectId").asLong());
            assertNotEquals("COMPLETED", project.get("status").asText());
            assertEquals(project.at("/schedule/calculatedFinishDate").asText(), upcoming.get("calculatedFinishDate").asText());
            assertEquals(project.at("/schedule/feasible").asBoolean(), upcoming.get("feasible").asBoolean());
        }

        JsonNode project = getJson("/projects/" + projectId);
        assertTrue(containsProject(summary.get("atRisk"), projectId));
        for (JsonNode atRisk : summary.get("atRisk"))
        {
            if (atRisk.get("projectId").asLong() == projectId)
            {
                assertEquals(project.get("deadline").asText(), atRisk.get("deadline").asText());
                assertEquals(project.at("/schedule/calculatedFinishDate").asText(),
                        atRisk.get("calculatedFinishDate").asText());
            }
        }
    }

    @Test
    @DisplayName("Get summary - should return the same capacity as the employee capacity endpoint")
    void getSummaryMatchesEmployeeCapacity()
    {
        createAsManager("/employee", """
                { "firstName": "Dashboard", "lastName": "Worker", "dailyCapacity": 7.5, "standardCapacity": false }
                """);

        JsonNode summary = getJson("/dashboard/summary");
        JsonNode capacity = getJson("/employee/capacity");

        assertEquals(capacity.get("employeeCount").asInt(), summary.at("/capacity/activeEmployees").asInt());
        assertEquals(capacity.get("dailyCapacity").asDouble(), summary.at("/capacity/dailyCapacity").asDouble(), 0.0001);
    }

    /**
     * Creates a project whose single task needs three working days but whose deadline is its start date.
     */
    private long createInfeasibleProjectWithTask()
    {
        long competenceId = createAsManager("/competences", """
                { "name": "dashboard competence %d", "rate": 850.00 }
                """.formatted(NAME_COUNTER.incrementAndGet()));
        long projectId = createAsManager("/projects", """
                { "title": "Dashboard project", "description": "Late project", "startDate": "2030-01-07", "deadline": "2030-01-07" }
                """);
        long stageId = createAsManager("/stages", """
                { "projectId": %d, "name": "Build" }
                """.formatted(projectId));
        createAsManager("/tasks", """
                { "stageId": %d, "name": "Walls", "competenceId": %d, "estimate": 16.0, "minimumDurationInDays": 2 }
                """.formatted(stageId, competenceId));
        return projectId;
    }

    private boolean containsProject(JsonNode projects, long projectId)
    {
        for (JsonNode project : projects)
        {
            if (project.get("projectId").asLong() == projectId)
            {
                return true;
            }
        }
        return false;
    }

    private JsonNode getJson(String path)
    {
        String response = givenManager()
                .when().get(path)
                .then().statusCode(200)
                .extract().asString();
        return readJson(response);
    }
}
