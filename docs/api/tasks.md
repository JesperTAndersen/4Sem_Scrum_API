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
| [`PUT`](#put-tasksidemployees) | `/tasks/{id}/employees` | Replace the employees assigned to a task |
| [`POST`](#post-taskstaskidemployeesemployeeid) | `/tasks/{taskId}/employees/{employeeId}` | Assign one employee to a task |
| [`DELETE`](#delete-taskstaskidemployeesemployeeid) | `/tasks/{taskId}/employees/{employeeId}` | Unassign one employee from a task |
| [`POST`](#post-taskstaskidpredecessorspredecessorid) | `/tasks/{taskId}/predecessors/{predecessorId}` | Add a predecessor dependency |
| [`DELETE`](#delete-taskstaskidpredecessorspredecessorid) | `/tasks/{taskId}/predecessors/{predecessorId}` | Remove a predecessor dependency |

## Task object

| Field | Type | Description |
|---|---|---|
| `id` | number | Unique identifier |
| `name` | string | Task name |
| `minimumDurationInDays` | integer | Non-negative minimum scheduled duration in working days |
| `competence` | object\|null | Assigned competence with only `id` and `name`; `null` when none is assigned |
| `estimate` | number | Estimated labor hours |
| `startDate`, `endDate` | date | Calculated Monday-Friday schedule dates; `endDate` is inclusive |
| `laborDurationInDays` | number | Estimated labor duration (`estimate / 7.5`) |
| `scheduledDurationInDays` | number | Greater of labor duration and minimum duration |
| `status` | enum | `NOT_STARTED`, `IN_PROGRESS`, or `DONE` |
| `predecessorIds` | number[] | IDs of tasks that must finish before this task can start |
| `dependencyStartOffsetInDays` | number | Earliest start offset based on predecessor durations; `0` when there are no predecessors |
| `dependencyFinishOffsetInDays` | number | Start offset plus the task scheduled duration |
| `assignedEmployees` | [Employee reference](#employee-reference-object)[] | Employees assigned to the task, ordered by ID; empty when none are assigned |

```json
{
  "id": 1,
  "name": "Requirements",
  "minimumDurationInDays": 2,
  "competence": { "id": 1, "name": "carpenter" },
  "estimate": 16.0,
  "laborDurationInDays": 2.1333333333333333,
  "scheduledDurationInDays": 2.1333333333333333,
  "status": "NOT_STARTED",
  "predecessorIds": [],
  "dependencyStartOffsetInDays": 0.0,
  "dependencyFinishOffsetInDays": 2.1333333333333333,
  "assignedEmployees": [
    { "id": 1, "firstName": "Alice", "lastName": "Worker", "active": true, "dailyCapacity": 7.5 }
  ]
}
```

## Employee reference object

A compact view of an assigned employee. It does not include the employee's competences; use [`GET /employee/{id}`](employee.md) for the full Employee object.

| Field | Type | Description |
|---|---|---|
| `id` | number | Employee ID |
| `firstName`, `lastName` | string | Employee name |
| `active` | boolean | Whether the employee is currently active |
| `dailyCapacity` | number | Effective hours per working day |

## Slim task object

Tasks nested in a stage or project response include only `id`, `name`, calculated `startDate` and `endDate`, `status`, dependency fields (`predecessorIds`, `dependencyStartOffsetInDays`, and `dependencyFinishOffsetInDays`), and `assignedEmployees`.

---

## GET /tasks

**Success response:** `200 OK` with `Task[]`.

## POST /tasks

Creates a task in an existing stage. The assigned competence must be active. New tasks start with the status `NOT_STARTED`.

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `stageId` | number | yes | Existing stage ID |
| `name` | string | yes | Not blank; surrounding whitespace is trimmed |
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
| `name` | string | no | Not blank when supplied; surrounding whitespace is trimmed |
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
| `400` | `id` or supplied field values are invalid, an estimate is supplied without a competence ID, the competence is inactive, or an assigned employee lacks the new competence |
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

## PUT /tasks/{id}/employees

Replaces the complete set of employees assigned to a task. Send every employee that should be assigned; employees left out are unassigned. Send an empty list to remove all assignments. Duplicate IDs are stored once.

The whole request is validated before anything is saved, so an invalid request leaves the existing assignments unchanged.

Assignments do not affect task duration yet; see #156. See [Assignment rules](#assignment-rules) for which employees can be assigned.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | number | Task ID |

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `employeeIds` | number[] | yes | Positive IDs of existing employees; may be empty |

```json
{ "employeeIds": [1, 2] }
```

**Success response:** `200 OK` with the updated Task object, including `assignedEmployees`.

**Errors**

| Status | When |
|---|---|
| `400` | `id` is invalid, the body or `employeeIds` is missing, an employee ID is not a positive number, a new employee is inactive or lacks the task's competence, or the task is `DONE` |
| `404` | The task or an employee does not exist |

## POST /tasks/{taskId}/employees/{employeeId}

Assigns one employee to a task and keeps the existing assignments. Use [`PUT /tasks/{id}/employees`](#put-tasksidemployees) to change several assignments at once.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `taskId` | number | Task ID |
| `employeeId` | number | Employee to assign |

**Success response:** `204 No Content`.

**Errors**

| Status | When |
|---|---|
| `400` | IDs are invalid, the employee is already assigned, inactive or lacks the task's competence, or the task is `DONE` |
| `404` | The task or employee does not exist |

## DELETE /tasks/{taskId}/employees/{employeeId}

Unassigns one employee from a task and keeps the other assignments.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `taskId` | number | Task ID |
| `employeeId` | number | Employee to unassign |

**Success response:** `204 No Content`.

**Errors**

| Status | When |
|---|---|
| `400` | IDs are invalid, the employee is not assigned to the task, or the task is `DONE` |
| `404` | The task or employee does not exist |

## POST /tasks/{taskId}/predecessors/{predecessorId}

Adds a predecessor to a task. Both tasks must belong to the same stage. A task cannot depend on itself, duplicate dependencies are rejected, and a dependency that would create a circular chain is rejected.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `taskId` | number | Task that will depend on another task |
| `predecessorId` | number | Task that must be completed first |

**Success response:** `204 No Content`.

**Errors**

| Status | When |
|---|---|
| `400` | IDs are invalid, tasks are from different stages, dependency already exists, task depends on itself, or the dependency would create a cycle |
| `404` | Either task does not exist |

## DELETE /tasks/{taskId}/predecessors/{predecessorId}

Removes an existing predecessor dependency.

**Success response:** `204 No Content`.

**Errors**

| Status | When |
|---|---|
| `400` | IDs are invalid or the dependency does not exist |
| `404` | Either task does not exist |

## Assignment rules

These rules apply to every endpoint that changes assignments (`PUT /tasks/{id}/employees`, `POST` and `DELETE /tasks/{taskId}/employees/{employeeId}`):

- A **new** assignment requires an **active** employee.
- A **new** assignment requires the employee to hold the task's competence. Tasks without a competence accept any active employee.
- An employee who is already assigned is not a new assignment: a deactivated employee can stay in a `PUT` request and can always be removed (unless the task is `DONE`).
- Assignments on a `DONE` task cannot be changed, so completed work keeps its staffing history.
- An employee is stored once per task, regardless of duplicate IDs or how many competences they hold.
- Changing a task's competence through `PUT /tasks/{id}` is rejected while an assigned employee lacks the new competence. Unassign those employees first; assignments are never removed automatically. Removing the competence (`competenceId: 0`) keeps the assignments.

## Dependency scheduling

Dependency offsets are calculated from the task graph. A task without predecessors starts at offset `0`. A dependent task starts after the predecessor with the latest calculated finish, so multiple tasks with the same predecessor can still run in parallel.

The offsets are relative planning values. Converting them into actual Monday-Friday calendar dates is handled by the working-day scheduling story (US-14).

## Task start and completion rules

Task predecessors must be in the **same stage**. Stage predecessors order work across stages. Both `IN_PROGRESS` and `DONE` status transitions reject unfinished task predecessors or predecessor stages (`400`), including direct `NOT_STARTED` to `DONE` attempts. Adding task predecessors after a task has started is rejected.

`predecessorIds` continues to describe task dependencies only. Stage-level dependencies are returned in the owning Stage object. Changes to any predecessor's duration or stage dependencies are reflected in computed task schedule responses.

**Reopening:** reopening a `DONE` task is rejected if a dependent task or a dependent stage has already started. Adding a new unfinished task to a completed stage is rejected when dependent stage work has begun. Directly updating completed tasks with `DONE` is subject to predecessor validation.
