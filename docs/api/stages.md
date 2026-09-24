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

## Stage object

| Field | Type | Description |
|---|---|---|
| `id` | number | Unique identifier |
| `name` | string | Stage name |
| `totalEstimatedHours` | number | Sum of its task estimates |
| `totalCost` | number | Sum of its task costs |
| `tasks` | [Task](tasks.md#task-object)[] | Tasks in the stage |

## GET /stages

**Success response:** `200 OK` with `Stage[]`.

## POST /stages

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `projectId` | number | yes | Existing project ID |
| `name` | string | yes | Not blank |

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
| `name` | string | yes | Not blank |

```json
{ "name": "Implementation" }
```

**Success response:** `200 OK` with a Stage object.  
**Errors:** `400` for invalid input; `404` when the stage does not exist.

## DELETE /stages/{id}

**Path parameters:** `id` — stage ID.  
**Success response:** `204 No Content`.  
**Errors:** `400` for an invalid ID; `404` when the stage does not exist.