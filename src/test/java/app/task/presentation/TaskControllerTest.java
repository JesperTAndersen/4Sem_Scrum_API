package app.task.presentation;

import static app.config.ApiTestExtension.UNKNOWN_ID;
import static app.config.ApiTestExtension.createAsManager;
import static app.config.ApiTestExtension.givenManager;
import static app.config.ApiTestExtension.givenToken;
import static app.config.ApiTestExtension.readJson;
import static app.config.ApiTestExtension.tokenFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.config.ApiTestExtension;
import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(ApiTestExtension.class)
class TaskControllerTest
{
    // Competence names are unique, so every created competence gets its own name.
    private static final AtomicInteger NAME_COUNTER = new AtomicInteger();
    private static final String VALID_UPDATE_BODY = """
            { "status": "IN_PROGRESS" }
            """;
    private static final String VALID_EMPLOYEES_BODY = """
            { "employeeIds": [] }
            """;

    @ParameterizedTest(name = "[{index}] {5}")
    @CsvFileSource(resources = "/testcases/task/task-controller/endpoint-access.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("All endpoints - require a PROJECT_MANAGER token and an existing task")
    void enforcesEndpointAccess(String method, String path, String target, String token, int expected, String reason)
    {
        String resolvedPath = path;
        if (path.contains("{employeeId}"))
        {
            long taskId = target.equals("UNKNOWN") ? UNKNOWN_ID : createTask("Access task", 1);
            long employeeId = target.equals("UNKNOWN") ? createEmployee("Access") : createEmployee("Access", taskId);
            if (method.equals("DELETE") && target.equals("EXISTING"))
            {
                assignEmployee(taskId, employeeId, 204);
            }
            resolvedPath = path.replace("{id}", String.valueOf(taskId))
                    .replace("{employeeId}", String.valueOf(employeeId));
        }
        else if (path.contains("{predecessorId}"))
        {
            long stageId = createStage();
            long predecessorId = createTask("Access predecessor", 1, stageId);
            long taskId = target.equals("UNKNOWN") ? UNKNOWN_ID : createTask("Access task", 1, stageId);
            if (method.equals("DELETE") && target.equals("EXISTING"))
            {
                addPredecessor(taskId, predecessorId, 204);
            }
            resolvedPath = path.replace("{id}", String.valueOf(taskId))
                    .replace("{predecessorId}", String.valueOf(predecessorId));
        }
        else
        {
            resolvedPath = path.replace("{id}", targetId(target));
        }

        RequestSpecification request = givenToken(tokenFor(token));
        if (method.equals("POST") && path.equals("/tasks"))
        {
            request.body(validCreateBody());
        }
        else if (method.equals("PUT") && path.endsWith("/employees"))
        {
            request.body(VALID_EMPLOYEES_BODY);
        }
        else if (method.equals("PUT"))
        {
            request.body(VALID_UPDATE_BODY);
        }

        request.when().request(method, resolvedPath).then().statusCode(expected);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/task/task-controller/path-id.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Endpoints with an id - reject an invalid path id")
    void rejectsInvalidPathId(String method, String path, int expected, String reason)
    {
        RequestSpecification request = givenManager();
        if (method.equals("PUT"))
        {
            request.body(VALID_UPDATE_BODY);
        }

        request.when().request(method, path).then().statusCode(expected);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/task/task-controller/create-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Create - rejects an invalid request")
    void createValidatesRequest(String body, int expected, String reason)
    {
        RequestSpecification request = givenManager();
        if (body != null)
        {
            request.body(body.replace("{stageId}", String.valueOf(createStage()))
                    .replace("{competenceId}", String.valueOf(createCompetence())));
        }

        request.when().post("/tasks").then().statusCode(expected);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/task/task-controller/update-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Update - rejects an invalid request")
    void updateValidatesRequest(String body, int expected, String reason)
    {
        long taskId = createTask("Update request task", 1);
        RequestSpecification request = givenManager();
        if (body != null)
        {
            request.body(body);
        }

        request.when().put("/tasks/" + taskId).then().statusCode(expected);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/task/task-controller/predecessors-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Add and remove predecessor - reject an invalid dependency")
    void predecessorsValidateRequest(String method, String relation, int expected, String reason)
    {
        long taskId = createTask("Dependency request task", 1);
        long predecessorId = relation.equals("SELF") ? taskId : createTask("Dependency request predecessor", 1);

        givenManager().when().request(method, "/tasks/" + taskId + "/predecessors/" + predecessorId)
                .then().statusCode(expected);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/task/task-controller/employees-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Replace employees - rejects an invalid request")
    void employeesValidateRequest(String body, int expected, String reason)
    {
        long taskId = createTask("Employees request task", 1);
        RequestSpecification request = givenManager();
        if (body != null)
        {
            request.body(body.replace("{employeeId}", String.valueOf(createEmployee("Request", taskId)))
                    .replace("{unknownEmployeeId}", String.valueOf(UNKNOWN_ID)));
        }

        request.when().put("/tasks/" + taskId + "/employees").then().statusCode(expected);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/task/task-controller/employee-assignment-request.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Assign and unassign employee - reject an invalid assignment")
    void employeeAssignmentValidatesRequest(String method, String relation, int expected, String reason)
    {
        long taskId = createTask("Assignment request task", 1);
        long employeeId = relation.equals("UNKNOWN") ? UNKNOWN_ID : createEmployee("Assignment", taskId);
        if (relation.equals("ASSIGNED"))
        {
            assignEmployee(taskId, employeeId, 204);
        }

        givenManager().when().request(method, "/tasks/" + taskId + "/employees/" + employeeId)
                .then().statusCode(expected);
    }

    @Test
    @DisplayName("Assign and unassign employee - should update one assignment and keep the others")
    void singleEmployeeCanBeAssignedAndUnassigned()
    {
        long taskId = createTask("Single assignment task", 1);
        long keptEmployeeId = createEmployee("Single kept", taskId);
        long removedEmployeeId = createEmployee("Single removed", taskId);

        assignEmployee(taskId, keptEmployeeId, 204);
        assignEmployee(taskId, removedEmployeeId, 204);
        assertEquals(2, getTask(taskId).get("assignedEmployees").size());

        givenManager().when().delete("/tasks/" + taskId + "/employees/" + removedEmployeeId)
                .then().statusCode(204);

        JsonNode employees = getTask(taskId).get("assignedEmployees");
        assertEquals(1, employees.size());
        assertEquals(keptEmployeeId, employees.get(0).get("id").asLong());
    }

    @Test
    @DisplayName("Assignment eligibility - an inactive employee should not be assigned through PUT or POST")
    void inactiveEmployeeIsRejected()
    {
        long taskId = createTask("Inactive assignment task", 1);
        long employeeId = createEmployee("Inactive", taskId);
        givenManager().when().patch("/employee/" + employeeId + "/deactivate").then().statusCode(204);

        replaceEmployees(taskId, "[%d]".formatted(employeeId), 400);
        assignEmployee(taskId, employeeId, 400);

        assertEquals(0, getTask(taskId).get("assignedEmployees").size());
    }

    @Test
    @DisplayName("Assignment eligibility - an employee without the task's competence should not be assigned")
    void employeeWithoutRequiredCompetenceIsRejected()
    {
        long taskId = createTask("Competence assignment task", 1);
        long employeeId = createEmployee("Unqualified");

        replaceEmployees(taskId, "[%d]".formatted(employeeId), 400);
        assignEmployee(taskId, employeeId, 400);

        assertEquals(0, getTask(taskId).get("assignedEmployees").size());
    }

    @Test
    @DisplayName("Assignment eligibility - a deactivated employee already assigned should stay on the task")
    void deactivatedAssignedEmployeeIsRetained()
    {
        long taskId = createTask("Retained assignment task", 1);
        long employeeId = createEmployee("Retained", taskId);
        replaceEmployees(taskId, "[%d]".formatted(employeeId), 200);
        givenManager().when().patch("/employee/" + employeeId + "/deactivate").then().statusCode(204);

        replaceEmployees(taskId, "[%d]".formatted(employeeId), 200);

        JsonNode employee = getTask(taskId).get("assignedEmployees").get(0);
        assertEquals(employeeId, employee.get("id").asLong());
        assertEquals(false, employee.get("active").asBoolean());
    }

    @Test
    @DisplayName("Assignment eligibility - assignments on a completed task should be locked")
    void completedTaskAssignmentsAreLocked()
    {
        long taskId = createTask("Completed assignment task", 1);
        long assignedId = createEmployee("Completed assigned", taskId);
        long otherId = createEmployee("Completed other", taskId);
        replaceEmployees(taskId, "[%d]".formatted(assignedId), 200);
        givenManager().body("{ \"status\": \"DONE\" }")
                .when().put("/tasks/" + taskId)
                .then().statusCode(200);

        assignEmployee(taskId, otherId, 400);
        givenManager().when().delete("/tasks/" + taskId + "/employees/" + assignedId).then().statusCode(400);
        replaceEmployees(taskId, "[]", 400);

        assertEquals(1, getTask(taskId).get("assignedEmployees").size());
    }

    @Test
    @DisplayName("Competence change - should be rejected while an assigned employee lacks the new competence")
    void competenceChangeIsRejectedWhileAssignedEmployeesLackIt()
    {
        long taskId = createTask("Competence change task", 1);
        long employeeId = createEmployee("Competence change", taskId);
        replaceEmployees(taskId, "[%d]".formatted(employeeId), 200);
        long oldCompetenceId = getTask(taskId).get("competence").get("id").asLong();
        long newCompetenceId = createCompetence();

        givenManager().body("{ \"competenceId\": %d, \"estimate\": 8.0 }".formatted(newCompetenceId))
                .when().put("/tasks/" + taskId)
                .then().statusCode(400);

        JsonNode task = getTask(taskId);
        assertEquals(oldCompetenceId, task.get("competence").get("id").asLong());
        assertEquals(1, task.get("assignedEmployees").size());
    }

    @Test
    @DisplayName("Replace employees - should store several employees and return them after reload")
    void employeesCanBeAssignedAndAreReturnedOnTask()
    {
        long taskId = createTask("Staffed task", 1);
        long firstEmployeeId = createEmployee("First", taskId);
        long secondEmployeeId = createEmployee("Second", taskId);

        replaceEmployees(taskId, "[%d, %d]".formatted(secondEmployeeId, firstEmployeeId), 200);

        JsonNode employees = getTask(taskId).get("assignedEmployees");
        assertEquals(2, employees.size());
        assertEquals(firstEmployeeId, employees.get(0).get("id").asLong());
        assertEquals("First", employees.get(0).get("firstName").asText());
        assertEquals("Worker", employees.get(0).get("lastName").asText());
        assertEquals(true, employees.get(0).get("active").asBoolean());
        assertEquals(secondEmployeeId, employees.get(1).get("id").asLong());
    }

    @Test
    @DisplayName("Replace employees - removing one employee should keep the others")
    void removingOneEmployeeKeepsTheOthers()
    {
        long taskId = createTask("Partially staffed task", 1);
        long keptEmployeeId = createEmployee("Kept", taskId);
        long removedEmployeeId = createEmployee("Removed", taskId);
        replaceEmployees(taskId, "[%d, %d]".formatted(keptEmployeeId, removedEmployeeId), 200);

        replaceEmployees(taskId, "[%d]".formatted(keptEmployeeId), 200);

        JsonNode employees = getTask(taskId).get("assignedEmployees");
        assertEquals(1, employees.size());
        assertEquals(keptEmployeeId, employees.get(0).get("id").asLong());
    }

    @Test
    @DisplayName("Replace employees - duplicate ids should be stored once")
    void duplicateEmployeeIdsAreStoredOnce()
    {
        long taskId = createTask("Duplicate staffed task", 1);
        long employeeId = createEmployee("Duplicate", taskId);

        replaceEmployees(taskId, "[%d, %d]".formatted(employeeId, employeeId), 200);

        assertEquals(1, getTask(taskId).get("assignedEmployees").size());
    }

    @Test
    @DisplayName("Replace employees - an invalid request should keep the existing assignments")
    void invalidEmployeesRequestKeepsExistingAssignments()
    {
        long taskId = createTask("Unchanged staffed task", 1);
        long employeeId = createEmployee("Unchanged", taskId);
        replaceEmployees(taskId, "[%d]".formatted(employeeId), 200);

        replaceEmployees(taskId, "[%d, %d]".formatted(employeeId, UNKNOWN_ID), 404);

        JsonNode employees = getTask(taskId).get("assignedEmployees");
        assertEquals(1, employees.size());
        assertEquals(employeeId, employees.get(0).get("id").asLong());
    }

    @Test
    @DisplayName("Get project - should include assigned employees for its tasks")
    void projectViewIncludesAssignedEmployees()
    {
        long taskId = createTask("Project view staffed task", 1);
        long employeeId = createEmployee("Project", taskId);
        replaceEmployees(taskId, "[%d]".formatted(employeeId), 200);

        String response = givenManager()
                .when().get("/projects/1")
                .then().statusCode(200)
                .extract().asString();

        JsonNode task = findTaskInProject(readJson(response), taskId);
        assertEquals(employeeId, task.get("assignedEmployees").get(0).get("id").asLong());
    }

    @Test
    @DisplayName("Duration - an unassigned task should use one assumed worker and require staffing")
    void unassignedTaskRequiresStaffing()
    {
        long taskId = createEstimatedTask("Unstaffed duration task", 30.0);

        JsonNode task = getTask(taskId);
        assertEquals("STAFFING_REQUIRED", task.get("staffingStatus").asText());
        assertEquals(4.0, task.get("laborDurationInDays").asDouble());
    }

    @Test
    @DisplayName("Duration - should be derived from the capacity of the assigned employees")
    void durationIsDerivedFromAssignedEmployees()
    {
        long taskId = createEstimatedTask("Staffed duration task", 30.0);
        long firstEmployeeId = createEmployee("Duration first", taskId);
        long secondEmployeeId = createEmployee("Duration second", taskId);

        replaceEmployees(taskId, "[%d, %d]".formatted(firstEmployeeId, secondEmployeeId), 200);

        JsonNode task = getTask(taskId);
        assertEquals("STAFFED", task.get("staffingStatus").asText());
        assertEquals(2.0, task.get("laborDurationInDays").asDouble());
        assertEquals(2.0, task.get("scheduledDurationInDays").asDouble());
    }

    @Test
    @DisplayName("Duration - should be undetermined for the task and project when no assigned employee is active")
    void deactivatedCrewMakesTaskAndProjectUndetermined()
    {
        long taskId = createEstimatedTask("Undetermined duration task", 30.0);
        try
        {
            long employeeId = createEmployee("Undetermined", taskId);
            replaceEmployees(taskId, "[%d]".formatted(employeeId), 200);
            givenManager().when().patch("/employee/" + employeeId + "/deactivate").then().statusCode(204);

            JsonNode task = getTask(taskId);
            assertEquals("NO_AVAILABLE_CAPACITY", task.get("staffingStatus").asText());
            assertTrue(isNull(task.get("laborDurationInDays")));
            assertTrue(isNull(task.get("scheduledDurationInDays")));
            assertTrue(isNull(task.get("startDate")));
            assertTrue(isNull(task.get("endDate")));

            String response = givenManager()
                    .when().get("/projects/1")
                    .then().statusCode(200)
                    .extract().asString();
            JsonNode schedule = readJson(response).get("schedule");
            assertTrue(isNull(schedule.get("calculatedFinishDate")));
            assertTrue(isNull(schedule.get("feasible")));
        }
        finally
        {
            // Project 1 is shared by the API tests, so remove the undetermined task again.
            givenManager().when().delete("/tasks/" + taskId).then().statusCode(204);
        }
    }

    @Test
    @DisplayName("Get all - should show the current status of each task")
    void taskListShowsCurrentStatusForEachTask()
    {
        long notStartedTaskId = createTask("Not started task", 1);
        long doneTaskId = createTask("Done task", 1);
        givenManager().body("{ \"status\": \"DONE\" }")
                .when().put("/tasks/" + doneTaskId)
                .then().statusCode(200);

        String response = givenManager()
                .when().get("/tasks")
                .then().statusCode(200)
                .extract().asString();

        JsonNode tasks = readJson(response);
        assertEquals("NOT_STARTED", findTask(tasks, notStartedTaskId).get("status").asText());
        assertEquals("DONE", findTask(tasks, doneTaskId).get("status").asText());
    }

    @Test
    @DisplayName("Add predecessor - should store the dependency and its offsets on the task")
    void predecessorCanBeAddedAndIsReturnedOnTask()
    {
        long stageId = createStage();
        long predecessorId = createTask("Dependency predecessor", 2, stageId);
        long taskId = createTask("Dependency task", 1, stageId);

        addPredecessor(taskId, predecessorId, 204);

        JsonNode task = getTask(taskId);
        assertEquals(1, task.get("predecessorIds").size());
        assertEquals(predecessorId, task.get("predecessorIds").get(0).asLong());
        assertEquals(2.0, task.get("dependencyStartOffsetInDays").asDouble());
        assertEquals(3.0, task.get("dependencyFinishOffsetInDays").asDouble());
    }

    @Test
    @DisplayName("Delete - should remove the deleted task from its dependents' predecessors")
    void deletingAPredecessorRemovesTheDependencyReference()
    {
        long stageId = createStage();
        long predecessorId = createTask("Deleted predecessor", 1, stageId);
        long taskId = createTask("Dependent task survives", 1, stageId);
        addPredecessor(taskId, predecessorId, 204);

        givenManager().when().delete("/tasks/" + predecessorId).then().statusCode(204);

        assertEquals(0, getTask(taskId).get("predecessorIds").size());
    }

    @Test
    @DisplayName("Get project - should include dependency data for its tasks")
    void projectViewIncludesDependencyAwareTaskData()
    {
        long stageId = createStage();
        long predecessorId = createTask("Project view predecessor", 2, stageId);
        long taskId = createTask("Project view dependent", 1, stageId);
        addPredecessor(taskId, predecessorId, 204);

        String response = givenManager()
                .when().get("/projects/1")
                .then().statusCode(200)
                .extract().asString();

        JsonNode task = findTaskInProject(readJson(response), taskId);
        assertEquals(predecessorId, task.get("predecessorIds").get(0).asLong());
        assertEquals(2.0, task.get("dependencyStartOffsetInDays").asDouble());
    }

    private JsonNode getTask(long taskId)
    {
        String response = givenManager()
                .when().get("/tasks/" + taskId)
                .then().statusCode(200)
                .extract().asString();
        return readJson(response);
    }

    private void addPredecessor(long taskId, long predecessorId, int expectedStatus)
    {
        givenManager()
                .when().post("/tasks/" + taskId + "/predecessors/" + predecessorId)
                .then().statusCode(expectedStatus);
    }

    private void assignEmployee(long taskId, long employeeId, int expectedStatus)
    {
        givenManager()
                .when().post("/tasks/" + taskId + "/employees/" + employeeId)
                .then().statusCode(expectedStatus);
    }

    private void replaceEmployees(long taskId, String employeeIds, int expectedStatus)
    {
        givenManager()
                .body("{ \"employeeIds\": %s }".formatted(employeeIds))
                .when().put("/tasks/" + taskId + "/employees")
                .then().statusCode(expectedStatus);
    }

    private long createEmployee(String firstName)
    {
        return createAsManager("/employee", """
                { "firstName": "%s", "lastName": "Worker", "dailyCapacity": 7.5, "standardCapacity": false }
                """.formatted(firstName));
    }

    private long createEmployee(String firstName, long taskId)
    {
        long employeeId = createEmployee(firstName);
        long competenceId = getTask(taskId).get("competence").get("id").asLong();
        givenManager()
                .body("{ \"competenceIds\": [%d] }".formatted(competenceId))
                .when().put("/employee/" + employeeId + "/competences")
                .then().statusCode(200);
        return employeeId;
    }

    private JsonNode findTaskInProject(JsonNode project, long taskId)
    {
        for (JsonNode stage : project.get("stages"))
        {
            for (JsonNode task : stage.get("tasks"))
            {
                if (task.get("id").asLong() == taskId)
                {
                    return task;
                }
            }
        }
        throw new AssertionError("Task not found in project: " + taskId);
    }

    private JsonNode findTask(JsonNode tasks, long taskId)
    {
        for (JsonNode task : tasks)
        {
            if (task.get("id").asLong() == taskId)
            {
                return task;
            }
        }
        throw new AssertionError("Task not found: " + taskId);
    }

    private String targetId(String target)
    {
        return switch (target)
        {
            case "EXISTING" -> String.valueOf(createTask("Access task", 1));
            case "UNKNOWN" -> String.valueOf(UNKNOWN_ID);
            default -> "";
        };
    }

    private long createTask(String name, int minimumDurationInDays)
    {
        return createTask(name, minimumDurationInDays, createStage());
    }

    private long createTask(String name, int minimumDurationInDays, long stageId)
    {
        return createAsManager("/tasks", """
                { "stageId": %d, "name": "%s", "competenceId": %d, "estimate": 0.0, "minimumDurationInDays": %d }
                """.formatted(stageId, name, createCompetence(), minimumDurationInDays));
    }

    private long createEstimatedTask(String name, double estimate)
    {
        return createAsManager("/tasks", """
                { "stageId": %d, "name": "%s", "competenceId": %d, "estimate": %s, "minimumDurationInDays": 0 }
                """.formatted(createStage(), name, createCompetence(), estimate));
    }

    private static boolean isNull(JsonNode node)
    {
        return node == null || node.isNull();
    }

    private String validCreateBody()
    {
        return """
                { "stageId": %d, "name": "Requirements", "competenceId": %d, "estimate": 16.0, "minimumDurationInDays": 2 }
                """.formatted(createStage(), createCompetence());
    }

    private long createStage()
    {
        return createAsManager("/stages", """
                { "projectId": 1, "name": "Task stage" }
                """);
    }

    private long createCompetence()
    {
        return createAsManager("/competences", """
                { "name": "task competence %d", "rate": 850.00 }
                """.formatted(NAME_COUNTER.incrementAndGet()));
    }
}
