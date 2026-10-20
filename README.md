# Project Estimation & Resource Planner

A web application for estimating projects and planning available resource capacity.

The application is designed for a project/resource manager to create projects with deadlines, divide them into dependent subprojects, estimate required manpower, and determine whether a project is feasible within the available time and quarterly resource capacity.

## Core Features

- Create and manage projects and subprojects
- Plan backwards from a project deadline
- Define dependencies between subprojects
- Calculate minimum project duration using working days
- Manage employees and competency labels
- Track available resource capacity per quarter
- Compare project requirements with available capacity
- Flag scheduling and resource-capacity conflicts
- Estimate project labor costs using hourly rates per competency
- View resource usage both by project and across multiple projects
- Simple user authentication

## Planning Model

The planner evaluates a project from three perspectives:

1. **Schedule** — Can the dependency chain be completed before the deadline?
2. **Capacity** — Are enough resource hours available for the required competencies?
3. **Cost** — What is the estimated labor cost of the project?

Projects that are not feasible are still allowed in the system but are clearly flagged, allowing the manager to adjust the deadline or project plan.

## Task assignment response contract

Task responses (`GET /api/v1/tasks`, `GET /api/v1/tasks/{id}`, task update responses,
and tasks nested in project/stage responses) include `assignedEmployees`, sorted by employee ID:

```json
{
  "assignedEmployees": [
    {
      "id": 42,
      "firstName": "Alex",
      "lastName": "Jensen",
      "active": false,
      "dailyCapacity": 7.5
    }
  ]
}
```

An unassigned task returns an empty array. Deactivating an employee preserves existing
assignments on both unfinished and completed tasks. Their identity and current active
status remain visible; task estimates, competencies, and project/stage relationships
are preserved. These references describe current employee details, not a snapshot at assignment time.

`dailyCapacity` is the employee's configured capacity, not their available contribution.
Inactive employees contribute no capacity to unfinished tasks or to `GET /api/v1/employee/capacity`.
An unfinished task with only inactive assignees has `staffingStatus: "NO_AVAILABLE_CAPACITY"`
and null durations/dates. Completed tasks retain the existing duration calculation using
all their assignees. Filtering employees for new staffing must not filter `assignedEmployees`.
New assignments reject inactive employees; replacing an unfinished task's assignments may
retain an already assigned inactive employee. Completed task assignments cannot be changed.
