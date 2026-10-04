package app.task.presentation;

import static app.config.ApiTestExtension.UNKNOWN_ID;
import static app.config.ApiTestExtension.createAsManager;
import static app.config.ApiTestExtension.givenManager;
import static app.config.ApiTestExtension.givenToken;
import static app.config.ApiTestExtension.readJson;
import static app.config.ApiTestExtension.tokenFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @ParameterizedTest(name = "[{index}] {5}")
    @CsvFileSource(resources = "/testcases/task/task-controller/endpoint-access.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("All endpoints - require a PROJECT_MANAGER token and an existing task")
    void enforcesEndpointAccess(String method, String path, String target, String token, int expected, String reason)
    {
        String resolvedPath = path;
        if (path.contains("{predecessorId}"))
        {
            long predecessorId = createTask("Access predecessor", 1);
            long taskId = target.equals("UNKNOWN") ? UNKNOWN_ID : createTask("Access task", 1);
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
        long predecessorId = createTask("Dependency predecessor", 2);
        long taskId = createTask("Dependency task", 1);

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
        long predecessorId = createTask("Deleted predecessor", 1);
        long taskId = createTask("Dependent task survives", 1);
        addPredecessor(taskId, predecessorId, 204);

        givenManager().when().delete("/tasks/" + predecessorId).then().statusCode(204);

        assertEquals(0, getTask(taskId).get("predecessorIds").size());
    }

    @Test
    @DisplayName("Get project - should include dependency data for its tasks")
    void projectViewIncludesDependencyAwareTaskData()
    {
        long predecessorId = createTask("Project view predecessor", 2);
        long taskId = createTask("Project view dependent", 1);
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
        return createAsManager("/tasks", """
                { "stageId": %d, "name": "%s", "competenceId": %d, "estimate": 0.0, "minimumDurationInDays": %d }
                """.formatted(createStage(), name, createCompetence(), minimumDurationInDays));
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
