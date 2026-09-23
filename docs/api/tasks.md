# Tasks

Tasks belong to a stage and define an estimate plus the competences needed to perform the work. A task cannot be moved to a different stage after creation.

**Base path:** `/api/v1/tasks`  
**Access:** `PROJECT_MANAGER`

| Method | Path | Summary |
|---|---|---|
| [`GET`](#get-tasks) | `/tasks` | List tasks |
| [`POST`](#post-tasks) | `/tasks` | Create a task |
| [`GET`](#get-tasksid) | `/tasks/{id}` | Get a task |
| [`PUT`](#put-tasksid) | `/tasks/{id}` | Update a task |
| [`DELETE`](#delete-tasksid) | `/tasks/{id}` | Delete a task |

## Task object

| Field | Type | Description |
|---|---|---|
| `id` | number | Unique identifier |
| `name` | string | Task name |
| `estimate` | number | Non-negative estimated hours |
| `minimumDurationInDays` | integer | Non-negative minimum scheduled duration in working days |
| `laborDurationInDays` | number | Calculated labor duration (`estimate / 8`) in working days |
| `scheduledDurationInDays` | number | Greater of labor duration and minimum duration |
| `status` | enum | `NOT_STARTED`, `IN_PROGESS`, or `DONE` |
| `competences` | [Competence](competences.md#competence-object)[] | Required competences |

## GET /tasks

**Success response:** `200 OK` with `Task[]`.

## POST /tasks

Creates a task in the supplied stage. Inactive competences cannot be assigned to a new task.

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `stageId` | number | yes | Existing stage ID |
| `name` | string | yes | Not blank |
| `estimate` | number | yes | Zero or greater |
| `minimumDurationInDays` | integer | yes | Zero or greater |
| `competenceIds` | number[] | yes | At least one existing, active competence |

```json
{ "stageId": 10, "name": "Requirements", "estimate": 8.0, "minimumDurationInDays": 1, "competenceIds": [1, 2] }
```

**Success response:** `201 Created` with a Task object.  
**Errors:** `400` for invalid input or inactive competence; `404` when a referenced stage or competence does not exist.

## GET /tasks/{id}

**Path parameters:** `id` — task ID.  
**Success response:** `200 OK` with a Task object.  
**Errors:** `400` for an invalid ID; `404` when the task does not exist.

## PUT /tasks/{id}

Updates task-owned fields; it does not change the task's stage. An existing task can retain a competence that was later deactivated.

**Path parameters:** `id` — task ID.

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `name` | string | yes | Not blank |
| `estimate` | number | yes | Zero or greater |
| `minimumDurationInDays` | integer | yes | Zero or greater |
| `competenceIds` | number[] | yes | At least one existing competence |

```json
{ "name": "Updated requirements", "estimate": 16.0, "minimumDurationInDays": 2, "competenceIds": [1, 2] }
```

**Success response:** `200 OK` with a Task object.  
**Errors:** `400` for invalid input; `404` when the task or a competence does not exist.

## DELETE /tasks/{id}

**Path parameters:** `id` — task ID.  
**Success response:** `204 No Content`.  
**Errors:** `400` for an invalid ID; `404` when the task does not exist.
