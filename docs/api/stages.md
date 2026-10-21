# Stages

Stages divide a project into named phases. A stage response always contains its tasks.

**Base path:** `/api/v1/stages`  
**Access:** `PROJECT_MANAGER`

| Method | Path | Summary |
|---|---|---|
| [`GET`](#get-stages) | `/stages` | List stages |
| [`POST`](#post-stages) | `/stages` | Create a stage |
| [`GET`](#get-stagesid) | `/stages/{id}` | Get a stage |
| [`PUT`](#put-stagesid) | `/stages/{id}` | Rename a stage |
| [`DELETE`](#delete-stagesid) | `/stages/{id}` | Delete a stage |
| `POST` | `/stages/{id}/predecessors/{predecessorId}` | Add a stage dependency |
| `DELETE` | `/stages/{id}/predecessors/{predecessorId}` | Remove a stage dependency |

## Stage object

| Field | Type | Description |
|---|---|---|
| `id` | number | Unique identifier |
| `name` | string | Stage name |
| `totalEstimatedHours` | number | Sum of its task estimates |
| `totalCost` | number | Sum of its task costs |
| `tasks` | [Task](tasks.md#task-object)[] | Tasks in the stage |
| `startDate`, `endDate` | date\|null | Earliest known task start and latest task finish (project start for empty stages). `endDate` is `null` when any task in the stage has an undetermined schedule (see [Staffing and duration](tasks.md#staffing-and-duration)) |
| `completed` | boolean | True when nonempty and all tasks are `DONE` |
| `predecessorIds` | number[] | IDs of stages that must finish before this stage starts |

Stage dates are aggregated from task dates. Unfinished tasks use live planning calculations; completed tasks with
snapshots use recorded dates and durations. The same values appear in task endpoints and project responses.
See [Completed planning snapshots](tasks.md#completed-planning-snapshots) for completion, reopening, and the
fallback for legacy tasks without snapshots.

## GET /stages

**Success response:** `200 OK` with `Stage[]`.

## POST /stages

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `projectId` | number | yes | Existing project ID |
| `name` | string | yes | Not blank; surrounding whitespace is trimmed |

```json
{ "projectId": 1, "name": "Planning" }
```

**Success response:** `201 Created` with a Stage object.  
**Errors:** `400` for invalid input; `404` when the project does not exist.

## GET /stages/{id}

**Path parameters:** `id` — stage ID.  
**Success response:** `200 OK` with a Stage object.  
**Errors:** `400` for an invalid ID; `404` when the stage does not exist.

## PUT /stages/{id}

**Path parameters:** `id` — stage ID.

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `name` | string | yes | Not blank; surrounding whitespace is trimmed |

```json
{ "name": "Implementation" }
```

**Success response:** `200 OK` with a Stage object.  
**Errors:** `400` for invalid input; `404` when the stage does not exist.

## DELETE /stages/{id}

**Path parameters:** `id` — stage ID.  
**Success response:** `204 No Content`.  
**Errors:** `400` for an invalid ID; `404` when the stage does not exist.
## Stage predecessor endpoints

`POST /stages/{id}/predecessors/{predecessorId}` adds a predecessor to the stage; `DELETE` on the same path removes it. Both return `204 No Content`.

The `GET /stages` and `GET /stages/{id}` responses include `predecessorIds` and `completed`.

Add rejects with `400`: different-project stages, self-dependency, duplicate, direct/indirect cycle, or a stage whose tasks already started. Remove rejects nonexistent dependencies with `400`. Missing IDs return `404`. Deleting a stage with dependent stages is rejected with `400`.

**Empty stages:** never completed. Their dates fall back to the project start; an empty predecessor imposes no scheduling delay, but blocks task progress until it contains finished tasks. Zero-duration tasks use their start as finish and impose no next-day delay.
