# Employee workload

Daily employee workload combines task assignments across all projects. Use over-allocation to find
capacity warnings and availability to find employees with hours left on a selected working day.

**Base path:** `/api/v1/employee`  
**Access:** `PROJECT_MANAGER`

See [authentication and errors](../API.md). Paths below are relative to `/api/v1`.

| Method | Path | Summary |
|---|---|---|
| [`GET`](#get-employeeover-allocations) | `/employee/over-allocations` | Find daily cross-project capacity conflicts |
| [`GET`](#get-employeeavailability) | `/employee/availability` | Read daily employee availability, optionally filtered by competence |

## Over-allocation result object

| Field | Type | Description |
|---|---|---|
| `startDate`, `endDate` | date | Requested inclusive period |
| `conflicts` | [Conflict](#conflict-object)[] | One entry per overloaded employee and working day, ordered by employee ID then date |
| `unassessedTaskIds` | number[] | Sorted task IDs excluded from the calculation; see [unassessed tasks](#unassessed-tasks) |

## Conflict object

| Field | Type | Description |
|---|---|---|
| `employeeId` | number | Employee identifier |
| `firstName`, `lastName` | string | Employee names |
| `date` | date | Working day on which capacity is exceeded |
| `availableHours` | number | Total configured daily capacity, not remaining hours |
| `allocatedHours` | number | Combined assigned hours for this employee on this day across all projects |
| `excessHours` | number | Assigned hours above daily capacity |
| `taskHours` | [Task hours](#task-hours-object)[] | Tasks contributing to this day's workload, ordered by task ID |

## Task hours object

| Field | Type | Description |
|---|---|---|
| `projectId` | number | Project identifier |
| `projectTitle` | string | Project title |
| `taskId` | number | Task identifier |
| `taskName` | string | Task name |
| `allocatedHours` | number | This task's assigned hours for this employee on this day |

## Availability result object

| Field | Type | Description |
|---|---|---|
| `date` | date | Requested day |
| `employees` | [Employee availability](#employee-availability-object)[] | Active employees matching the optional competence filter, ordered by employee ID |
| `unassessedTaskIds` | number[] | Sorted task IDs excluded from the calculation across all projects; not filtered by competence |

## Employee availability object

| Field | Type | Description |
|---|---|---|
| `employeeId` | number | Employee identifier |
| `firstName`, `lastName` | string | Employee names |
| `dailyCapacity` | number | Total configured hours per working day |
| `allocatedHours` | number | Combined assigned hours for this employee on the requested day across all projects |
| `remainingHours` | number | Daily capacity minus assigned hours, with a minimum of zero |

## GET /employee/over-allocations

Both dates are required and included in the calculation. There is no project or competence filter:
assignments across all projects are considered together for each employee.

| Query parameter | Type | Required | Rules |
|---|---|---|---|
| `startDate` | date | yes | Valid date in `YYYY-MM-DD` format |
| `endDate` | date | yes | Same format; cannot be before `startDate` |

```http
GET /api/v1/employee/over-allocations?startDate=2030-01-07&endDate=2030-01-07
Authorization: Bearer <token>
```

**Success response:** `200 OK` with an [Over-allocation result](#over-allocation-result-object).

```json
{
    "startDate": "2030-01-07",
    "endDate": "2030-01-07",
    "conflicts": [
        {
            "employeeId": 42,
            "firstName": "Alex",
            "lastName": "Jensen",
            "date": "2030-01-07",
            "availableHours": 7.5,
            "allocatedHours": 10.0,
            "excessHours": 2.5,
            "taskHours": [
                {
                    "projectId": 1,
                    "projectTitle": "Project A",
                    "taskId": 10,
                    "taskName": "Implementation",
                    "allocatedHours": 7.5
                },
                {
                    "projectId": 2,
                    "projectTitle": "Project B",
                    "taskId": 20,
                    "taskName": "Review",
                    "allocatedHours": 2.5
                }
            ]
        }
    ],
    "unassessedTaskIds": []
}
```

No detected conflicts returns `conflicts: []`. A weekend-only range is valid and returns no conflicts.
Equal capacity is not a conflict; excess below `0.000001` hours is ignored. Each day is checked
independently, so spare capacity on another day does not cancel an overloaded day.

**Errors**

| Status | When |
|---|---|
| `400` | Either date is missing, blank or invalid, or `endDate` precedes `startDate` |

## GET /employee/availability

Returns active employees and their remaining hours for one day. Employees without assignments are
included, as are fully booked and overloaded employees with `remainingHours: 0`.

| Query parameter | Type | Required | Rules |
|---|---|---|---|
| `date` | date | yes | Valid date in `YYYY-MM-DD` format |
| `competenceId` | number | no | Positive integer ID; omit to include all active employees |

```http
GET /api/v1/employee/availability?date=2030-01-07&competenceId=3
Authorization: Bearer <token>
```

**Success response:** `200 OK` with an [Availability result](#availability-result-object).

```json
{
    "date": "2030-01-07",
    "employees": [
        {
            "employeeId": 42,
            "firstName": "Alex",
            "lastName": "Jensen",
            "dailyCapacity": 7.5,
            "allocatedHours": 10.0,
            "remainingHours": 0.0
        },
        {
            "employeeId": 43,
            "firstName": "Sara",
            "lastName": "Nielsen",
            "dailyCapacity": 7.5,
            "allocatedHours": 0.0,
            "remainingHours": 7.5
        }
    ],
    "unassessedTaskIds": []
}
```

The example employees both hold competence `3`. The filter selects employees; their workload still
includes assignments requiring any competence. Existing inactive competence labels can also be used
as a filter. Omitting the filter includes active employees without competences.

An unknown competence ID or no matching active employees returns `employees: []`, not `404`.
This differs from the competence filter on [employee listing and capacity](employee.md).
Inactive employees are excluded. Saturday and Sunday return `employees: []`.
The competence filter does not narrow `unassessedTaskIds`; unassessed tasks can still be returned
when the employee list is empty.

**Errors**

| Status | When |
|---|---|
| `400` | `date` is missing, blank or invalid, or a supplied `competenceId` is blank, nonnumeric, outside the positive integer ID range, zero or negative |

## Calculation rules

Each task's estimated hours are divided between its active eligible assignees in proportion to their
daily capacity, then spread evenly over its full scheduled working days. Requesting a shorter period
does not concentrate the whole estimate into that period. Task dates follow the existing dependency
and minimum-duration scheduling rules described in [tasks](tasks.md).

For example, a 30-hour task with employees at 7.5 and 2.5 hours/day and a three-day schedule contributes
7.5 and 2.5 hours per day respectively. A five-day minimum duration spreads those shares to 4.5 and
1.5 hours per day instead.

Capacity uses each employee's current custom or company-standard setting. Only active assignees
holding the task's required competence contribute. Completed tasks and zero estimates create no
workload; in-progress tasks use their whole estimate. Completed predecessors still affect unfinished
task dates. No project status or deadline filter hides scheduled work. Weekends are excluded.

Hours are returned as calculated numbers without display rounding. Clients should format them to
two decimal places while keeping every returned conflict visible. The existing
[`GET /employee/capacity`](employee.md#get-employeecapacity) reports total configured capacity before
assignments; availability subtracts assessed assignments for a selected day.

## Unassessed tasks

Both responses include IDs of unfinished tasks with positive estimates whose staffing or dates could
not be determined. An empty conflict list means no conflict was found among assessed tasks. Remaining
hours are also based only on assessed tasks. When `unassessedTaskIds` is nonempty, show a warning that
workload and availability may be incomplete.

Tasks with known dates outside the requested day or period are excluded. Tasks with undetermined
dates remain unassessed regardless of the requested period because their overlap cannot be established.
Use [`GET /tasks/{id}`](tasks.md#get-tasksid) to inspect them and their existing staffing warnings.

## Saving and refreshing warnings

Over-allocation is a warning and does not reject otherwise valid assignment, task or project saves.
After saving, request conflicts for the displayed period and availability for the selected day again.
Both endpoints recalculate from persisted data. Conflicts remain visible after reload and disappear
when assignments or dates resolve them. Removing an assignment may clear a conflict while leaving the
task unassessed until it is staffed. Normal save validation still applies.
