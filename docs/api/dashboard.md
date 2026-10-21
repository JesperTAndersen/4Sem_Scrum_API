# Dashboard

All paths are relative to `/api/v1`. All dashboard endpoints require the `PROJECT_MANAGER` role.
See [authentication and errors](../API.md).

| Method | Path | Description |
|---|---|---|
| GET | `/dashboard/summary` | Aggregated planning statistics for all projects, tasks and employee capacity |

## GET /dashboard/summary

**Success response:** `200 OK`:

```json
{
    "projects": {
        "total": 6,
        "byStatus": { "DRAFT": 1, "PLANNED": 2, "IN_PROGRESS": 2, "COMPLETED": 1 },
        "infeasible": 1
    },
    "totalEstimatedHours": 1240.0,
    "totalCost": 800123.00,
    "remainingHours": 740.0,
    "tasks": { "notStarted": 14, "inProgress": 5, "done": 21 },
    "capacity": { "activeEmployees": 4, "dailyCapacity": 30.0 },
    "atRisk": [
        { "projectId": 3, "title": "Villa", "deadline": "2026-10-28", "calculatedFinishDate": "2026-11-04" }
    ],
    "upcomingDeadlines": [
        { "projectId": 1, "title": "Kitchen", "deadline": "2026-10-30", "calculatedFinishDate": "2026-10-27", "feasible": true }
    ]
}
```

| Field | Type | Description |
|---|---|---|
| `projects.total` | number | Number of projects in every status |
| `projects.byStatus` | object | Number of projects per status; every status is present, with `0` when unused |
| `projects.infeasible` | number | Number of projects in `atRisk` |
| `totalEstimatedHours` | number | Sum of `totalEstimatedHours` of active projects |
| `totalCost` | number | Sum of `totalCost` of active projects |
| `remainingHours` | number | Sum of `estimate` of tasks that are not `DONE` in active projects |
| `tasks` | object | Number of tasks per status across every project |
| `capacity.activeEmployees` | number | Number of active employees |
| `capacity.dailyCapacity` | number | Sum of the daily hours of active employees |
| `atRisk` | object[] | Active projects whose plan cannot meet the deadline, ordered by deadline |
| `upcomingDeadlines` | object[] | At most five active projects, ordered by deadline |
| `upcomingDeadlines[].feasible` | boolean \| null | `true` when the plan meets the deadline, `false` when it does not, `null` when undetermined |

Rules:

- The statistics are calculated from persisted data on every request. Totals, task counts, `calculatedFinishDate`
  and `feasible` are the same values as `GET /projects/{id}` returns for the same project.
- An active project is a project whose status is not `COMPLETED`. Completed projects are counted in `projects` and
  `tasks`, but are left out of `totalEstimatedHours`, `totalCost`, `remainingHours`, `atRisk`, `infeasible` and
  `upcomingDeadlines`. The totals therefore describe the work that is planned or in progress.
- `remainingHours` is the estimated work that is not finished yet. Dividing it by `capacity.dailyCapacity` gives an
  estimate of how many working days of work the active employees have ahead of them.
- `atRisk` and `upcomingDeadlines` are ordered by deadline, then by project ID. `upcomingDeadlines` is not filtered by
  today's date, so an active project with a passed deadline is still listed.
- A project with an undetermined schedule (see [projects](projects.md)) is not at risk, because it cannot be evaluated
  against its deadline. It is listed in `upcomingDeadlines` with `calculatedFinishDate` and `feasible` set to `null`.
- `capacity` uses the same rules as [`GET /employee/capacity`](employee.md#get-employeecapacity) without a filter:
  only active employees contribute.
- An empty database returns zero values and empty lists.

**Errors:** `401` without a valid token; `403` for a user without the `PROJECT_MANAGER` role.
