# Tasks

Tasks belong to a stage and have one required competence plus an estimated number of labor hours.

**Base path:** `/api/v1/tasks`  
**Access:** `PROJECT_MANAGER`

| Method | Path | Summary |
|---|---|---|
| GET | `/tasks` | List tasks |
| POST | `/tasks` | Create a task |
| GET | `/tasks/{id}` | Get a task |
| PUT | `/tasks/{id}` | Update a task |
| DELETE | `/tasks/{id}` | Delete a task |

## Task object

| Field | Type | Description |
|---|---|---|
| `id` | number | Unique identifier |
| `name` | string | Task name |
| `minimumDurationInDays` | integer | Non-negative minimum scheduled duration in working days |
| `competenceId` | number | Assigned competence ID; `0` means none after an update |
| `estimate` | number | Estimated labor hours |
| `laborDurationInDays` | number | Estimated labor duration (`estimate / 7.5`) |
| `scheduledDurationInDays` | number | Greater of labor duration and minimum duration |
| `status` | enum | `NOT_STARTED`, `IN_PROGRESS`, or `DONE` |

## POST /tasks

Creates a task. All fields are required; `estimate` must be a non-negative number of hours and `minimumDurationInDays` must be a non-negative whole number.

```json
{ "stageId": 10, "name": "Requirements", "competenceId": 1, "estimate": 16.0, "minimumDurationInDays": 2 }
```

## PUT /tasks/{id}

Updates any supplied task-owned fields. To assign a competence, provide both `competenceId` and `estimate`; use `competenceId: 0` to remove it.

```json
{ "competenceId": 1, "estimate": 16.0 }
```

## DELETE /tasks/{id}

Deletes a task and returns `204 No Content`.
