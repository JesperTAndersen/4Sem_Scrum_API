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

## Task object

| Field | Type | Description |
|---|---|---|
| `id` | number | Unique identifier |
| `name` | string | Task name |
| `minimumDurationInDays` | integer | Non-negative minimum scheduled duration in working days |
| `competence` | object\|null | Assigned competence with only `id` and `name`; `null` when none is assigned |
| `estimate` | number | Estimated labor hours |
| `laborDurationInDays` | number | Estimated labor duration (`estimate / 7.5`) |
| `scheduledDurationInDays` | number | Greater of labor duration and minimum duration |
| `status` | enum | `NOT_STARTED`, `IN_PROGRESS`, or `DONE` |

```json
{
  "id": 1,
  "name": "Requirements",
  "minimumDurationInDays": 2,
  "competence": { "id": 1, "name": "carpenter" },
  "estimate": 16.0,
  "laborDurationInDays": 2.1333333333333333,
  "scheduledDurationInDays": 2.1333333333333333,
  "status": "NOT_STARTED"
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
