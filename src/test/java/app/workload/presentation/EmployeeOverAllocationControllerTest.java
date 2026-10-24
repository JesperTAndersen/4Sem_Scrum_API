package app.workload.presentation;

import app.config.ApiTestExtension;
import app.config.HibernateTestConfig;
import app.persistence.testutils.TestPopulator;
import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.concurrent.atomic.AtomicInteger;

import static app.config.ApiTestExtension.createAsManager;
import static app.config.ApiTestExtension.givenManager;
import static app.config.ApiTestExtension.givenToken;
import static app.config.ApiTestExtension.readJson;
import static app.config.ApiTestExtension.tokenFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(ApiTestExtension.class)
class EmployeeOverAllocationControllerTest
{
    private static final String PATH = "/employee/over-allocations";
    private static final AtomicInteger NAME_COUNTER = new AtomicInteger();

    @BeforeEach
    void resetProjects()
    {
        TestPopulator.populateProjects(HibernateTestConfig.getEntityManagerFactory());
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(resources = "/testcases/planning/employee-over-allocation-controller/get-request.csv",
            numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Get conflicts - validates access and required inclusive dates")
    void validatesRequest(String startDate, String endDate, String token, int expected, String reason)
    {
        RequestSpecification request = givenToken(tokenFor(token));
        if (startDate != null)
        {
            request.queryParam("startDate", startDate);
        }
        if (endDate != null)
        {
            request.queryParam("endDate", endDate);
        }
        JsonNode result = readJson(request.when().get(PATH).then().statusCode(expected).extract().asString());
        if (expected == 200)
        {
            assertEquals(startDate, result.get("startDate").asText());
            assertEquals(endDate, result.get("endDate").asText());
            assertEquals(0, result.get("conflicts").size());
            assertEquals(0, result.get("unassessedTaskIds").size());
        }
        else
        {
            assertEquals(expected, result.get("status").asInt());
            assertEquals("/api/v1" + PATH, result.get("path").asText());
            assertFalse(result.get("message").asText().isBlank());
            assertFalse(result.get("errorId").asText().isBlank());
            assertFalse(result.get("timestamp").asText().isBlank());
        }
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @CsvFileSource(resources = "/testcases/planning/employee-over-allocation-controller/save-reload-resolve.csv",
            numLinesToSkip = 1)
    @DisplayName("Get conflicts - valid saves retain cross-project conflicts until resolved")
    void savesReloadsAndResolvesConflicts(String resolution, String reason)
    {
        long competenceId = createCompetence();
        long employeeId = createEmployee(competenceId);
        long firstProjectId = createProject("First project");
        long secondProjectId = createProject("Second project");
        long firstTaskId = createTask(firstProjectId, competenceId, 37.5);
        long secondTaskId = createTask(secondProjectId, competenceId, 12.5);
        assignEmployee(firstTaskId, employeeId);
        assignEmployee(secondTaskId, employeeId);

        JsonNode result = getConflicts("2030-01-07", "2030-01-07");
        assertEquals(1, result.get("conflicts").size());
        assertEquals(0, result.get("unassessedTaskIds").size());
        JsonNode conflict = result.get("conflicts").get(0);
        assertEquals(employeeId, conflict.get("employeeId").asLong());
        assertEquals("Shared", conflict.get("firstName").asText());
        assertEquals("Worker", conflict.get("lastName").asText());
        assertEquals("2030-01-07", conflict.get("date").asText());
        assertEquals(7.5, conflict.get("availableHours").asDouble());
        assertEquals(10, conflict.get("allocatedHours").asDouble());
        assertEquals(2.5, conflict.get("excessHours").asDouble());
        JsonNode taskHours = conflict.get("taskHours");
        assertEquals(2, taskHours.size());
        assertEquals(firstProjectId, taskHours.get(0).get("projectId").asLong());
        assertEquals("First project", taskHours.get(0).get("projectTitle").asText());
        assertEquals(firstTaskId, taskHours.get(0).get("taskId").asLong());
        assertEquals("Planned work", taskHours.get(0).get("taskName").asText());
        assertEquals(7.5, taskHours.get(0).get("allocatedHours").asDouble());
        assertEquals(secondProjectId, taskHours.get(1).get("projectId").asLong());
        assertEquals(secondTaskId, taskHours.get(1).get("taskId").asLong());
        assertEquals(2.5, taskHours.get(1).get("allocatedHours").asDouble());

        assertEquals(result, getConflicts("2030-01-07", "2030-01-07"));
        assertEquals(5, getConflicts("2030-01-07", "2030-01-11").get("conflicts").size());
        assertEquals(0, getConflicts("2030-01-12", "2030-01-13").get("conflicts").size());
        if (resolution.equals("REMOVE_ASSIGNMENT"))
        {
            givenManager().when().delete("/tasks/" + secondTaskId + "/employees/" + employeeId)
                    .then().statusCode(204);
        }
        else
        {
            givenManager().body("""
                    { "title": "Second project", "description": "Moved to resolve conflict",
                      "startDate": "2030-01-14", "deadline": "2030-02-01", "status": "DRAFT" }
                    """).when().put("/projects/" + secondProjectId).then().statusCode(200);
        }
        JsonNode resolved = getConflicts("2030-01-07", "2030-01-11");
        assertEquals(0, resolved.get("conflicts").size());
        if (resolution.equals("REMOVE_ASSIGNMENT"))
        {
            assertEquals(1, resolved.get("unassessedTaskIds").size());
            assertEquals(secondTaskId, resolved.get("unassessedTaskIds").get(0).asLong());
        }
        else
        {
            assertEquals(0, resolved.get("unassessedTaskIds").size());
        }
        assertEquals(resolved, getConflicts("2030-01-07", "2030-01-11"));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/planning/employee-over-allocation-controller/get-unassessed.csv",
            numLinesToSkip = 1)
    @DisplayName("Get conflicts - exposes unassessed tasks with context without fabricating allocations")
    void reportsUnassessedTasks(String staffing, String expectedReason, String reason)
    {
        long competenceId = createCompetence();
        long taskId = createTask(createProject("Unassessed project"), competenceId, 37.5);
        if (staffing.equals("INACTIVE"))
        {
            long employeeId = createEmployee(competenceId);
            assignEmployee(taskId, employeeId);
            givenManager().when().patch("/employee/" + employeeId + "/deactivate").then().statusCode(204);
        }
        JsonNode result = getConflicts("2030-01-07", "2030-01-11");
        assertEquals(0, result.get("conflicts").size());
        assertEquals(1, result.get("unassessedTaskIds").size());
        assertEquals(taskId, result.get("unassessedTaskIds").get(0).asLong());

        JsonNode unassessed = result.get("unassessedTasks").get(0);
        assertEquals(taskId, unassessed.get("taskId").asLong());
        assertEquals("Planned work", unassessed.get("taskName").asText());
        assertEquals("Unassessed project", unassessed.get("projectTitle").asText());
        assertEquals(expectedReason, unassessed.get("reason").asText());
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/planning/employee-over-allocation-controller/get-availability.csv",
            numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Get availability - requires a project manager and a valid date")
    void validatesAvailabilityRequest(String date, String token, int expected, String reason, String competenceId)
    {
        RequestSpecification request = givenToken(tokenFor(token));
        if (date != null)
        {
            request.queryParam("date", date);
        }
        if (competenceId != null)
        {
            request.queryParam("competenceId", competenceId);
        }
        JsonNode result = readJson(request.when().get("/employee/availability")
                .then().statusCode(expected).extract().asString());
        if (expected == 200)
        {
            assertEquals(date, result.get("date").asText());
            assertEquals(0, result.get("unassessedTaskIds").size());
            if ("999999".equals(competenceId))
            {
                assertEquals(0, result.get("employees").size());
            }
            if (date.equals("2030-01-12") || date.equals("2030-01-13"))
            {
                assertEquals(0, result.get("employees").size());
            }
        }
        else
        {
            assertEquals(expected, result.get("status").asInt());
            assertEquals("/api/v1/employee/availability", result.get("path").asText());
            assertFalse(result.get("message").asText().isBlank());
        }
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get availability - reads saved assignments across projects and includes idle employees")
    void readsEmployeeAvailability()
    {
        long competenceId = createCompetence();
        long employeeId = createEmployee(competenceId);
        long idleId = createEmployee(competenceId);
        long inactiveId = createEmployee(competenceId);
        givenManager().when().patch("/employee/" + inactiveId + "/deactivate").then().statusCode(204);
        long firstTask = createTask(createProject("Availability first"), competenceId, 12.5);
        assignEmployee(firstTask, employeeId);

        JsonNode initial = getAvailability();
        JsonNode employee = availabilityFor(initial, employeeId);
        assertEquals("Shared", employee.get("firstName").asText());
        assertEquals("Worker", employee.get("lastName").asText());
        assertEquals(7.5, employee.get("dailyCapacity").asDouble());
        assertEquals(2.5, employee.get("allocatedHours").asDouble());
        assertEquals(5, employee.get("remainingHours").asDouble());
        assertEquals(0, availabilityFor(initial, idleId).get("allocatedHours").asDouble());
        assertEquals(7.5, availabilityFor(initial, idleId).get("remainingHours").asDouble());
        for (JsonNode row : initial.get("employees"))
        {
            org.junit.jupiter.api.Assertions.assertNotEquals(inactiveId, row.get("employeeId").asLong());
        }

        long secondTask = createTask(createProject("Availability second"), competenceId, 37.5);
        assignEmployee(secondTask, employeeId);
        JsonNode overloaded = getAvailability();
        assertEquals(10, availabilityFor(overloaded, employeeId).get("allocatedHours").asDouble());
        assertEquals(0, availabilityFor(overloaded, employeeId).get("remainingHours").asDouble());
        assertEquals(overloaded, getAvailability());

        givenManager().when().delete("/tasks/" + secondTask + "/employees/" + employeeId)
                .then().statusCode(204);
        JsonNode resolved = getAvailability();
        assertEquals(5, availabilityFor(resolved, employeeId).get("remainingHours").asDouble());
        assertEquals(1, resolved.get("unassessedTaskIds").size());
        assertEquals(secondTask, resolved.get("unassessedTaskIds").get(0).asLong());
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get availability - filters competence without hiding work requiring another competence")
    void filtersAvailabilityByCompetence()
    {
        long requiredId = createCompetence();
        long otherId = createCompetence();
        long matchingId = createEmployee(requiredId);
        givenManager().body("{ \"competenceIds\": [%d, %d] }".formatted(requiredId, otherId))
                .when().put("/employee/" + matchingId + "/competences").then().statusCode(200);
        long idleId = createEmployee(requiredId);
        createEmployee(otherId);
        long taskId = createTask(createProject("Work using other competence"), otherId, 12.5);
        assignEmployee(taskId, matchingId);
        long unassessedId = createTask(createProject("Unassigned work"), otherId, 7.5);

        JsonNode result = readJson(givenManager().queryParam("date", "2030-01-07")
                .queryParam("competenceId", requiredId).when().get("/employee/availability")
                .then().statusCode(200).extract().asString());
        assertEquals(2, result.get("employees").size());
        JsonNode matching = availabilityFor(result, matchingId);
        assertEquals(2.5, matching.get("allocatedHours").asDouble());
        assertEquals(5, matching.get("remainingHours").asDouble());
        assertEquals(2, matching.get("competences").size());
        assertEquals(requiredId, matching.get("competences").get(0).get("id").asLong());
        assertEquals(7.5, availabilityFor(result, idleId).get("remainingHours").asDouble());
        assertEquals(1, result.get("unassessedTaskIds").size());
        assertEquals(unassessedId, result.get("unassessedTaskIds").get(0).asLong());
    }

    private JsonNode getAvailability()
    {
        return readJson(givenManager().queryParam("date", "2030-01-07").when()
                .get("/employee/availability").then().statusCode(200).extract().asString());
    }

    private JsonNode availabilityFor(JsonNode result, long employeeId)
    {
        for (JsonNode employee : result.get("employees"))
        {
            if (employee.get("employeeId").asLong() == employeeId)
            {
                return employee;
            }
        }
        throw new AssertionError("Missing availability for employee " + employeeId);
    }

    private JsonNode getConflicts(String startDate, String endDate)
    {
        return readJson(givenManager().queryParam("startDate", startDate).queryParam("endDate", endDate)
                .when().get(PATH).then().statusCode(200).extract().asString());
    }

    private long createProject(String title)
    {
        return createAsManager("/projects", """
                { "title": "%s", "description": "Over-allocation test",
                  "startDate": "2030-01-07", "deadline": "2030-02-01" }
                """.formatted(title));
    }

    private long createCompetence()
    {
        return createAsManager("/competences", """
                { "name": "Over-allocation competence %d", "rate": 850.00 }
                """.formatted(NAME_COUNTER.incrementAndGet()));
    }

    private long createEmployee(long competenceId)
    {
        long id = createAsManager("/employee", """
                { "firstName": "Shared", "lastName": "Worker", "dailyCapacity": 7.5,
                  "standardCapacity": false }
                """);
        givenManager().body("{ \"competenceIds\": [%d] }".formatted(competenceId))
                .when().put("/employee/" + id + "/competences").then().statusCode(200);
        return id;
    }

    private long createTask(long projectId, long competenceId, double estimate)
    {
        long stageId = createAsManager("/stages", """
                { "projectId": %d, "name": "Work stage" }
                """.formatted(projectId));
        return createAsManager("/tasks", """
                { "stageId": %d, "name": "Planned work", "competenceId": %d,
                  "bestEstimate": %s, "likelyEstimate": %s, "worstEstimate": %s, "minimumDurationInDays": 5 }
                """.formatted(stageId, competenceId, estimate, estimate, estimate));
    }

    private void assignEmployee(long taskId, long employeeId)
    {
        givenManager().when().post("/tasks/" + taskId + "/employees/" + employeeId).then().statusCode(204);
    }
}
