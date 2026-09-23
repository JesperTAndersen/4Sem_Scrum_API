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
| `minDuration` | number | Non-negative minimum duration hours |
| `competenceId` | number | [competence](competences.md) ID, 0 == not set |
| `estimate` | number | Non-negative estimated hours for competence |
| `status` | enum | `NOT_STARTED`, `IN_PROGRESS`, or `DONE` |

## GET /tasks

**Success response:** `200 OK` with `Task[]`.

## POST /tasks

Creates a task in the supplied stage. Inactive competences cannot be assigned to a new task.

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `stageId` | number | yes | Existing stage ID |
| `name` | string | yes | Not blank |
| `minDuration` | number | yes | Zero or greater |

```json
{ "stageId": 10, "name": "Requirements", "minDuration": 8.0, "competenceId" : 0, "estimate" : 0.0 }
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
| `name` | string | no | Not blank |
| `minDuration` | number | no | Zero or greater |
| `competenceId` | number | no | Existing competence and is active. Set to 0 to remove competence |
| `estimate` | number | yes if competenceId != 0 | Non-negative estimate in hours for competence |

```json
{ "name": "Update Requirements", "minDuration": 16.0, "competenceId" : 1, "estimate" : 6.0 }
```

**Success response:** `200 OK` with a Task object.  
**Errors:** `400` for invalid input; `404` when the task or a competence does not exist.

## DELETE /tasks/{id}

**Path parameters:** `id` — task ID.  
**Success response:** `204 No Content`.  
**Errors:** `400` for an invalid ID; `404` when the task does not exist.