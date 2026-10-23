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

| Field | Type | Description                                                                                                                                                                                                                 |
|---|---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `id` | number | Unique identifier                                                                                                                                                                                                           |
| `name` | string | Task name                                                                                                                                                                                                                   |
| `minimumDurationInDays` | integer | Non-negative minimum scheduled duration in working days                                                                                                                                                                     |
| `competence` | object\|null | Assigned competence with only `id` and `name`; `null` when none is assigned                                                                                                                                                 |
| `estimate` | number | Estimated labor hours                                                                                                                                                                                                       |
| `cost` | number | Current estimate multiplied by the assigned competence's rate; `0` without a competence                                                                                                                                     |
| `startDate`, `endDate` | date\|null | Monday-Friday planning dates; `endDate` is inclusive. Recorded for completed tasks with a snapshot, otherwise calculated live. `null` when undetermined (see [Completed planning snapshots](#completed-planning-snapshots)) |
| `laborDurationInDays` | number\|null | Recorded labor duration for completed tasks with a snapshot; otherwise calculated from current staffing (see [Staffing and duration](#staffing-and-duration)). `null` when undetermined                                     |
| `scheduledDurationInDays` | number\|null | Recorded scheduled duration for completed tasks with a snapshot; otherwise the greater of live labor duration and minimum duration. `null` when undetermined                                                                |
| `status` | enum | `NOT_STARTED`, `IN_PROGRESS`, or `DONE`                                                                                                                                                                                     |
| `staffingStatus` | enum | `STAFFED`, `STAFFING_REQUIRED`, or `NO_AVAILABLE_CAPACITY` (see [Staffing and duration](#staffing-and-duration))                                                                                                            |
| `staffingWarnings` | object[] | Current warnings identifying the task and affected employee; empty for completed tasks (see [Staffing warnings](#staffing-warnings))                                                                                        |
| `predecessorIds` | number[] | IDs of tatasks that must finish before this task can start                                                                                                                                                                  |
| `dependencyStartOffsetInDays` | number\|null | Earliest start offset based on predecessor durations; `0` when there are no predecessors; `null` when a predecessor is undetermined                                                                                         |
| `dependencyFinishOffsetInDays` | number\|null | Start offset plus the task scheduled duration; `null` when undetermined                                                                                                                                                     |
| `assignedEmployees` | [Employee reference](#employee-reference-object)[] | Employees assigned to the task, ordered by ID; empty when none are assigned                                                                                                                                                 |

Example completed task for a project starting on `2030-01-07`:

```json
{
  "id": 1,
  "name": "Requirements",
  "minimumDurationInDays": 5,
  "competence": { "id": 1, "name": "carpenter" },
  "estimate": 30.0,
  "cost": 25500.00,
  "startDate": "2030-01-07",
  "endDate": "2030-01-11",
  "laborDurationInDays": 4.0,
  "scheduledDurationInDays": 5.0,
  "status": "DONE",
  "staffingStatus": "STAFFED",
  "staffingWarnings": [],
  "predecessorIds": [],
  "dependencyStartOffsetInDays": 0.0,
  "dependencyFinishOffsetInDays": 5.0,
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

## Tasks in stage and project responses

Tasks nested in a stage or project response use the same [Task object](#task-object) as task endpoints,
including the recorded dates and durations described in [Completed planning snapshots](#completed-planning-snapshots).

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

Entering `DONE` records the calculated planning dates and durations in the same transaction as the status change.
Estimate, competence, and minimum-duration changes in that request are applied before the snapshot is calculated.
Sending `DONE` for an already completed task preserves its existing snapshot. See
[Completed planning snapshots](#completed-planning-snapshots) and [reopening rules](#task-start-and-completion-rules).

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
| `400` | `id` or supplied field values are invalid, an estimate is supplied without a competence ID, the competence is inactive, an assigned employee lacks the new competence, predecessors are unfinished when starting/completing, or reopening is blocked by started dependents |
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

Assignment changes recalculate unfinished task duration and scheduling; see [Staffing and duration](#staffing-and-duration).
See [Assignment rules](#assignment-rules) for which employees can be assigned.

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

## Staffing and duration

For an unfinished task (`NOT_STARTED` or `IN_PROGRESS`), labor duration is calculated from the employees assigned to it:

```
laborDurationInDays = estimate / combined daily capacity of the active eligible assigned employees
```

Each employee contributes their effective daily capacity once (custom capacity, or the company standard capacity when `standardCapacity` is `true`), regardless of how many competences they hold. The scheduled duration is the greater of the labor duration and `minimumDurationInDays`; working-day rounding and dependency ordering are unchanged.

| `staffingStatus` | When | Labor duration |
|---|---|---|
| `STAFFED` | Active eligible assignees provide positive combined capacity | `estimate / combined capacity` |
| `STAFFING_REQUIRED` | No employees are assigned | `estimate / 7.5` (one assumed worker; no named employee is allocated) |
| `NO_AVAILABLE_CAPACITY` | Employees are assigned, but none provides usable capacity | `null` (undetermined) |

Examples:

- 30 hours, two active employees at 7.5 hours/day: 30 / 15 = **2 days**.
- 30 hours, employees at 7.5 and 2.5 hours/day: 30 / 10 = **3 days**.
- 30 hours, no employees assigned: 30 / 7.5 = **4 days**, `STAFFING_REQUIRED`.

**Undetermined schedules.** An unfinished task with `NO_AVAILABLE_CAPACITY` has no duration and no dates.
Unfinished tasks that depend on it, directly or through a predecessor stage, are also undetermined.
Affected stage end dates and the project's `calculatedFinishDate` and `feasible` become `null` as well
(see [stages](stages.md) and [projects](projects.md)). The plan can still be saved; restoring active capacity
recalculates unfinished work. Completed tasks with snapshots keep their recorded values.

## Staffing warnings

Every task response includes `staffingWarnings`, including tasks nested in stage and project responses.
Warnings are derived from current assignments on each read and are ordered by employee ID:

```json
{
  "staffingStatus": "STAFFED",
  "staffingWarnings": [
    { "warningType": "INACTIVE_ASSIGNED_EMPLOYEE", "taskId": 12, "employeeId": 42 }
  ]
}
```

| `warningType` | Meaning |
|---|---|
| `INACTIVE_ASSIGNED_EMPLOYEE` | An unfinished task retains an inactive employee; that employee contributes zero capacity |
| `ASSIGNED_EMPLOYEE_MISSING_COMPETENCE` | An active assignee no longer holds the task's required competence and contributes zero capacity |

Eligibility means holding the task's required competence; tasks without a competence accept any active employee.
Assignments are retained after competence loss. `IN_PROGRESS` uses the whole estimate because remaining hours
are not tracked. An inactive assignee receives only the inactive warning, even if they also lack the competence.
A partially active crew can be `STAFFED` and still have warnings. Reactivation clears the inactive warning;
capacity resumes if the employee is eligible. Restoring a missing competence clears the eligibility warning.
Unassigned tasks and `DONE` tasks return an empty array. Completed assignments and snapshots remain visible.
Warnings do not block saving a plan and are not persisted separately. The project `schedule.staffingWarnings`
aggregates warnings from all unfinished tasks, ordered by task ID then employee ID, in both detail and list responses.
Dependent tasks can have undetermined dates without their own staffing warning; the originating task's warning
remains visible in the project schedule.

## Completed planning snapshots

When an unfinished task transitions to `DONE`, its calculated `startDate`, `endDate`, `laborDurationInDays`,
and `scheduledDurationInDays` are recorded atomically with the status change. These are planning values at
completion, not actual work dates or tracked hours. A failed save commits neither the snapshot nor the status change.

While the task remains `DONE`, reads use these recorded values. Employee deactivation, custom or company-standard
capacity changes, project start-date changes, and recalculation do not replace them. Updating an already completed
task's estimate or minimum duration also leaves the snapshot unchanged.

Task endpoints and tasks nested in stage/project responses use the same recorded values. Stage dates and project
finish/feasibility calculations include the recorded task dates. A completed predecessor supplies its recorded dates
and scheduled duration when an unfinished task is scheduled, including dependencies through predecessor stages.
Successors start on the next working day after its inclusive end date; a zero-duration predecessor allows the same day.

Completion is allowed when planning values are undetermined. The snapshot records those `null` values and keeps
them on subsequent reads, even if capacity later becomes available. Reopening resumes live calculation.

Only the four planning fields are snapshotted. Employee references, staffing status, estimates, and costs continue
to reflect current data. For `DONE` tasks, the current staffing-status calculation counts assigned employees even
when inactive; their assignments remain visible, but they contribute no capacity to unfinished work.
Snapshot storage fields and the recording timestamp are internal and are not separate request or response fields.

### Existing DONE tasks without snapshots

The compatibility policy is a live-calculation fallback for legacy `DONE` tasks without a recorded snapshot.
Their dates and durations are calculated from current data, counting all assigned employees, including inactive
employees. Those values may change when capacity or the surrounding plan changes; they are not reliable historical values.

There is no automatic backfill on deployment or on reads. Reading a legacy task or sending `DONE` again does not
create a snapshot. If reopening is allowed, reopening and completing it again records a new snapshot from that
current plan. Original completion-time values cannot be reconstructed reliably from the available data.

## Dependency scheduling

Dependency offsets are calculated from the task graph. A task without predecessors starts at offset `0`. A dependent task starts after the predecessor with the latest calculated finish, so multiple tasks with the same predecessor can still run in parallel.

Offsets are calculated from the current dependency graph, using recorded durations for completed tasks with snapshots.
The offsets themselves are not snapshotted and may change when the graph changes. Use `startDate` and `endDate`
for recorded calendar planning dates; scheduling uses a completed predecessor's recorded dates rather than
reconstructing them from these offsets.

## Task start and completion rules

Task predecessors must be in the **same stage**. Stage predecessors order work across stages. Both `IN_PROGRESS` and `DONE` status transitions reject unfinished task predecessors or predecessor stages (`400`), including direct `NOT_STARTED` to `DONE` attempts. Adding task predecessors after a task has started is rejected.

`predecessorIds` describes task dependencies only. Stage-level dependencies are returned in the owning Stage object.
Live predecessor-duration and stage-dependency changes affect unfinished task schedules. Completed tasks with
snapshots retain their recorded dates and durations.

**Reopening:** returning a `DONE` task to `NOT_STARTED` or `IN_PROGRESS` resumes live date and duration calculations
using current staffing, project dates, and dependencies. Its previous snapshot remains stored but is ignored while
the task is unfinished. Completing it again replaces the snapshot with the newly calculated planning values.

Reopening is rejected if a dependent task or a dependent stage has already started. Adding a new unfinished task
to a completed stage is rejected when dependent stage work has begun. Directly updating completed tasks with
`DONE` remains subject to predecessor validation and does not refresh a snapshot.
