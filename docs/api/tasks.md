# Tasks

Tasks belong to stages and describe estimated work. Each task is assigned an active competence and has a minimum scheduled duration.

**Base path:** `/api/v1/tasks`  
**Access:** `PROJECT_MANAGER`

| Method | Path | Summary |
|---|---|---|
| [`GET`](#get-tasks) | `/tasks` | List tasks |
| [`POST`](#post-tasks) | `/tasks` | Create a task |
| [`GET`](#get-tasksid) | `/tasks/{id}` | Get a task |
| [`PUT`](#put-tasksid) | `/tasks/{id}` | Update a task |
| [`DELETE`](#delete-tasksid) | `/tasks/{id}` | Delete a task |
| [`POST`](#post-taskstaskidpredecessorspredecessorid) | `/tasks/{taskId}/predecessors/{predecessorId}` | Add a predecessor dependency |
| [`DELETE`](#delete-taskstaskidpredecessorspredecessorid) | `/tasks/{taskId}/predecessors/{predecessorId}` | Remove a predecessor dependency |

## Task object

| Field | Type | Description |
|---|---|---|
| `id` | number | Unique identifier |
| `name` | string | Task name |
| `minimumDurationInDays` | integer | Non-negative minimum scheduled duration in working days |
| `competenceId` | number | Assigned competence ID; `0` when no competence is assigned |
| `estimate` | number | Estimated labor hours |
| `laborDurationInDays` | number | Estimated labor duration (`estimate / 7.5`) |
| `scheduledDurationInDays` | number | Greater of labor duration and minimum duration |
| `status` | enum | `NOT_STARTED`, `IN_PROGRESS`, or `DONE` |
| `predecessorIds` | number[] | IDs of tasks that must finish before this task can start |
| `dependencyStartOffsetInDays` | number | Earliest start offset based on predecessor durations; `0` when there are no predecessors |
| `dependencyFinishOffsetInDays` | number | Start offset plus the task scheduled duration |

```json
{
  "id": 1,
  "name": "Requirements",
  "minimumDurationInDays": 2,
  "competenceId": 1,
  "estimate": 16.0,
  "laborDurationInDays": 2.1333333333333333,
  "scheduledDurationInDays": 2.1333333333333333,
  "status": "NOT_STARTED",
  "predecessorIds": [],
  "dependencyStartOffsetInDays": 0.0,
  "dependencyFinishOffsetInDays": 2.1333333333333333
}
```

---

## GET /tasks

**Success response:** `200 OK` with `Task[]`.

## POST /tasks

Creates a task in an existing stage. The assigned competence must be active. New tasks start with the status `NOT_STARTED`.

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `stageId` | number | yes | Existing stage ID |
| `name` | string | yes | Not blank |
| `competenceId` | number | yes | Existing active competence ID |
| `estimate` | number | yes | Non-negative, finite number of labor hours |
| `minimumDurationInDays` | integer | yes | Non-negative whole number |

```json
{ "stageId": 10, "name": "Requirements", "competenceId": 1, "estimate": 16.0, "minimumDurationInDays": 2 }
```

**Success response:** `201 Created` with a Task object.

**Errors**

| Status | When |
|---|---|
| `400` | The request is missing a required field, contains invalid values, or assigns an inactive competence |
| `404` | The stage or competence does not exist |

## GET /tasks/{id}

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | number | Task ID |

**Success response:** `200 OK` with a Task object.

**Errors**

| Status | When |
|---|---|
| `400` | `id` is invalid |
| `404` | The task does not exist |

## PUT /tasks/{id}

Updates supplied task fields. To assign or change a competence, provide both `competenceId` and `estimate`. Set `competenceId` to `0` to remove the current competence; this also sets the estimate to `0`. The task status can be changed to `NOT_STARTED`, `IN_PROGRESS`, or `DONE`.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | number | Task ID |

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `name` | string | no | Not blank when supplied |
| `minimumDurationInDays` | integer | no | Non-negative whole number when supplied |
| `competenceId` | number | no | Existing active competence ID, or `0` to remove the assignment |
| `estimate` | number | no | Must accompany a non-zero `competenceId`; non-negative, finite number of labor hours |
| `status` | enum | no | `NOT_STARTED`, `IN_PROGRESS`, or `DONE` |

```json
{ "status": "IN_PROGRESS" }
```

**Success response:** `200 OK` with the updated Task object.

**Errors**

| Status | When |
|---|---|
| `400` | `id` or supplied field values are invalid, an estimate is supplied without a competence ID, or the competence is inactive |
| `404` | The task or supplied competence does not exist |

## DELETE /tasks/{id}

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | number | Task ID |

**Success response:** `204 No Content`.

**Errors**

| Status | When |
|---|---|
| `400` | `id` is invalid |
| `404` | The task does not exist |

## POST /tasks/{taskId}/predecessors/{predecessorId}

Adds a predecessor to a task. Both tasks must belong to the same project. A task cannot depend on itself, duplicate dependencies are rejected, and a dependency that would create a circular chain is rejected.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `taskId` | number | Task that will depend on another task |
| `predecessorId` | number | Task that must be completed first |

**Success response:** `204 No Content`.

**Errors**

| Status | When |
|---|---|
| `400` | IDs are invalid, tasks are from different projects, dependency already exists, task depends on itself, or the dependency would create a cycle |
| `404` | Either task does not exist |

## DELETE /tasks/{taskId}/predecessors/{predecessorId}

Removes an existing predecessor dependency.

**Success response:** `204 No Content`.

**Errors**

| Status | When |
|---|---|
| `400` | IDs are invalid or the dependency does not exist |
| `404` | Either task does not exist |

## Dependency scheduling

Dependency offsets are calculated from the task graph. A task without predecessors starts at offset `0`. A dependent task starts after the predecessor with the latest calculated finish, so multiple tasks with the same predecessor can still run in parallel.

The offsets are relative planning values. Converting them into actual Monday-Friday calendar dates is handled by the working-day scheduling story (US-14).
