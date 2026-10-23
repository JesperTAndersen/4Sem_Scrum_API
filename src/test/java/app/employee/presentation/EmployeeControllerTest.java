package app.employee.presentation;

import static app.config.ApiTestExtension.UNKNOWN_ID;
import static app.config.ApiTestExtension.createAsManager;
import static app.config.ApiTestExtension.givenManager;
import static app.config.ApiTestExtension.givenToken;
import static app.config.ApiTestExtension.readJson;
import static app.config.ApiTestExtension.tokenFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import app.config.ApiTestExtension;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(ApiTestExtension.class)
class EmployeeControllerTest
{
    // Competence names are unique, so every created competence gets its own name.
    private static final AtomicInteger NAME_COUNTER = new AtomicInteger();

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/employee/employee-controller/get-all-by-competence.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Get all by competence - requires a PROJECT_MANAGER token and a valid, existing competence id")
    void getAllByCompetenceValidatesRequest(String competenceId, String token, int expected, String reason)
    {
        String resolvedId = switch (competenceId)
        {
            case "EXISTING" -> String.valueOf(createCompetence());
            case "UNKNOWN" -> String.valueOf(UNKNOWN_ID);
            default -> competenceId;
        };

        givenToken(tokenFor(token))
                .when().get("/employee?competenceId=" + resolvedId)
                .then().statusCode(expected);
    }

    @Test
    @DisplayName("Get all by competence - should return only employees holding the competence, including inactive")
    void getAllByCompetenceReturnsMatchingEmployees()
    {
        long competenceId = createCompetence();
        long otherCompetenceId = createCompetence();
        long activeId = createEmployee("Active", competenceId);
        long inactiveId = createEmployee("Inactive", competenceId);
        long otherId = createEmployee("Other", otherCompetenceId);
        givenManager().when().patch("/employee/" + inactiveId + "/deactivate").then().statusCode(204);

        JsonNode employees = getEmployees("?competenceId=" + competenceId);

        List<Long> ids = new ArrayList<>();
        employees.forEach(employee -> ids.add(employee.get("id").asLong()));
        assertEquals(List.of(activeId, inactiveId), ids.stream().sorted().toList());
        assertFalse(ids.contains(otherId));
    }

    @Test
    @DisplayName("Get all by competence - should return an empty list for a competence without employees")
    void getAllByCompetenceWithoutEmployeesReturnsEmptyList()
    {
        long competenceId = createCompetence();

        assertEquals(0, getEmployees("?competenceId=" + competenceId).size());
    }

    @Test
    @DisplayName("Get all - should still return employees without a filter")
    void getAllWithoutFilterReturnsEmployees()
    {
        long employeeId = createEmployee("Unfiltered", createCompetence());

        List<Long> ids = new ArrayList<>();
        getEmployees("").forEach(employee -> ids.add(employee.get("id").asLong()));
        assertEquals(true, ids.contains(employeeId));
    }

    private JsonNode getEmployees(String query)
    {
        String response = givenManager()
                .when().get("/employee" + query)
                .then().statusCode(200)
                .extract().asString();
        return readJson(response);
    }

    private long createEmployee(String firstName, long competenceId)
    {
        long employeeId = createAsManager("/employee", """
                { "firstName": "%s", "lastName": "Worker", "dailyCapacity": 7.5, "standardCapacity": false }
                """.formatted(firstName));
        givenManager()
                .body("{ \"competenceIds\": [%d] }".formatted(competenceId))
                .when().put("/employee/" + employeeId + "/competences")
                .then().statusCode(200);
        return employeeId;
    }

    private long createCompetence()
    {
        return createAsManager("/competences", """
                { "name": "employee competence %d", "rate": 850.00 }
                """.formatted(NAME_COUNTER.incrementAndGet()));
    }
}
