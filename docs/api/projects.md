# Projects

Projects are top-level work containers. A project detail includes its stages and slim task summaries. Use `/tasks/{id}` for a task's full assignment, estimate, cost, and duration details.

**Base path:** `/api/v1/projects`  
**Access:** `PROJECT_MANAGER`

| Method | Path | Summary |
|---|---|---|
| [`GET`](#get-projects) | `/projects` | List project summaries |
| [`POST`](#post-projects) | `/projects` | Create a project |
| [`GET`](#get-projectsid) | `/projects/{id}` | Get the full project hierarchy |
| [`PUT`](#put-projectsid) | `/projects/{id}` | Replace project-owned fields |
| [`DELETE`](#delete-projectsid) | `/projects/{id}` | Delete a project |

## Project object

| Field | Type | Description |
|---|---|---|
| `id` | number | Unique identifier |
| `title`, `description` | string | Project text |
| `startDate`, `deadline` | date | Project schedule |
| `status` | enum | `DRAFT`, `PLANNED`, `IN_PROGRESS`, `COMPLETED` |
| `createdBy`, `updatedBy` | [user reference](users.md#user-reference) | Audit user |
| `createdAt`, `updatedAt` | date-time | Audit timestamps |
| `totalEstimatedHours` | number | Sum of estimates in all stages |
| `totalCost` | number | Sum of costs in all stages |
| `stages` | Stage[] | Full nested stage hierarchy |
| `schedule` | object | Calculated schedule: `calculatedFinishDate` and `feasible` |

Task dates are recalculated whenever a project is read. Scheduling starts at the project start date, excludes Saturday and Sunday, respects predecessor dependencies, and does not prevent saving a project whose plan is infeasible.

- A task occupies whole working days: a fractional duration rounds up (0.5 days takes one day).
- A task with a duration of 0 starts and ends on the same day, and its successors can start that day.
- `calculatedFinishDate` is the latest task end date, or the project start date when the project has no tasks.
- `feasible` is `true` when `calculatedFinishDate` is on or before the `deadline`.

## GET /projects

Returns project summaries. Each item includes `taskCountDTO` with `totalTaskCount` and `taskFinished`.

**Success response:** `200 OK` with `ProjectSlim[]`.

## POST /projects

Creates a project. New projects start with status `DRAFT`.

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `title` | string | yes | Not blank; at most 250 characters |
| `description` | string | no | At most 500 characters when supplied |
| `startDate` | date | yes | `YYYY-MM-DD` |
| `deadline` | date | yes | `YYYY-MM-DD`; not before `startDate` |

```json
{ "title": "Website redesign", "description": "Redesign the public website", "startDate": "2030-01-01", "deadline": "2030-04-01" }
```

**Success response:** `201 Created` with a Project object.  
**Errors:** `400` for invalid project data.

## GET /projects/{id}

**Path parameters**

| Name | Type | Description |
|---|---|---|
| `id` | number | Project ID |

**Success response:** `200 OK` with a Project object.  
**Errors:** `400` for an invalid ID; `404` when the project does not exist.

## PUT /projects/{id}

Updates all project-owned fields.

**Path parameters:** `id` — project ID.

**Request body**

| Field | Type | Required |
|---|---|---|
| `title`, `startDate`, `deadline`, `status` | string / date / date / enum | yes |
| `description` | string | no |

```json
{ "title": "Website redesign", "description": "Updated scope", "startDate": "2030-02-01", "deadline": "2030-06-01", "status": "PLANNED" }
```

**Success response:** `200 OK` with a Project object.  
**Errors:** `400` for invalid input; `404` when the project does not exist.

## DELETE /projects/{id}

**Path parameters:** `id` — project ID.  
**Success response:** `204 No Content`.  
**Errors:** `400` for an invalid ID; `404` when the project does not exist.
