# Competences

Competences represent skills and their hourly rates. Names are normalized to lowercase and are unique without regard to case.

**Base path:** `/api/v1/competences`  
**Access:** Authenticated

| Method | Path | Summary |
|---|---|---|
| [`GET`](#get-competences) | `/competences` | List competences |
| [`POST`](#post-competences) | `/competences` | Create a competence |
| [`GET`](#get-competencesid) | `/competences/{id}` | Get a competence |
| [`PUT`](#put-competencesid) | `/competences/{id}` | Update a competence |
| [`PATCH`](#patch-competencesidactivate) | `/competences/{id}/activate` | Activate a competence |
| [`PATCH`](#patch-competencesiddeactivate) | `/competences/{id}/deactivate` | Deactivate a competence |
| [`DELETE`](#delete-competencesid) | `/competences/{id}` | Delete an unused competence |

## Competence object

| Field | Type | Description |
|---|---|---|
| `id` | number | Unique identifier |
| `name` | string | Lowercase competence name |
| `rate` | number | Positive hourly rate |
| `active` | boolean | Whether it can be assigned to new tasks |
| `createdAt`, `updatedAt` | date-time | Audit timestamps |

```json
{ "id": 1, "name": "backend development", "rate": 850.00, "active": true, "createdAt": "2026-01-01T09:00:00", "updatedAt": "2026-01-02T10:00:00" }
```

## GET /competences

**Success response:** `200 OK` with `Competence[]`.

## POST /competences

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `name` | string | yes | Trimmed, lowercase; 2–100 valid text characters; unique |
| `rate` | number | yes | Positive JSON number, not a numeric string |

```json
{ "name": "Backend development", "rate": 850.00 }
```

**Success response:** `201 Created` with a Competence object.  
**Errors:** `400` for invalid input or a duplicate name.

## GET /competences/{id}

**Path parameters:** `id` — competence ID.  
**Success response:** `200 OK` with a Competence object.  
**Errors:** `400` for an invalid ID; `404` when the competence does not exist.

## PUT /competences/{id}

**Path parameters:** `id` — competence ID.  
**Request body:** Same fields and rules as [POST /competences](#post-competences).  
**Success response:** `200 OK` with a Competence object.  
**Errors:** `400` or `404` for invalid input (including a duplicate name) or a missing competence.

## PATCH /competences/{id}/activate

**Path parameters:** `id` — competence ID.  
**Success response:** `204 No Content`.  
**Errors:** `400` for an invalid ID; `404` when the competence does not exist.

## PATCH /competences/{id}/deactivate

Prevents new task assignments while preserving existing ones.

**Path parameters:** `id` — competence ID.  
**Success response:** `204 No Content`.  
**Errors:** `400` for an invalid ID; `404` when the competence does not exist.

## DELETE /competences/{id}

Only an unused competence can be deleted. Deactivate a competence that is referenced by a task.

**Path parameters:** `id` — competence ID.  
**Success response:** `204 No Content`.  
**Errors:** `400` for an invalid ID; `404` when the competence does not exist; `409` when it is in use.
