# Employees

All paths are relative to `/api/v1`. All employee endpoints require the `PROJECT_MANAGER` role.
See [authentication and errors](../API.md). The existing singular `/employee` path is retained for compatibility.

| Method | Path | Description |
|---|---|---|
| GET | `/employee` | List employees, including inactive employees |
| POST | `/employee` | Register an active employee |
| GET | `/employee/{id}` | Get an employee |
| PUT | `/employee/{id}` | Update employee information and daily capacity |
| PUT | `/employee/{id}/competences` | Replace employee competencies |
| GET | `/employee/capacity` | Calculate daily capacity of active employees |
| PATCH | `/employee/{id}/activate` | Activate an employee |
| PATCH | `/employee/{id}/deactivate` | Deactivate an employee |
| DELETE | `/employee/{id}` | Delete an employee |

## Employee object

| Field | Type | Description |
|---|---|---|
| `id` | number | Employee identifier |
| `firstName`, `lastName` | string | Trimmed names |
| `dailyCapacity` | number | Effective hours per working day |
| `standardCapacity` | boolean | Whether capacity comes from the company setting |
| `active` | boolean | Whether the employee contributes to daily availability |
| `competences` | [Competence](competences.md#competence-object)[] | Competencies, ordered by ID |
| `createdAt`, `updateAt` | date-time | Audit timestamps; the existing `updateAt` field name is retained |

```json
{
    "id": 1,
    "firstName": "Alice",
    "lastName": "Worker",
    "dailyCapacity": 6.0,
    "standardCapacity": false,
    "active": true,
    "competences": [],
    "createdAt": "2026-10-08T10:00:00",
    "updateAt": "2026-10-08T10:00:00"
}
```

## POST /employee

| Field | Type | Required | Rules |
|---|---|---|---|
| `firstName`, `lastName` | string | yes | 2-100 valid text characters after trimming |
| `dailyCapacity` | number | when using custom capacity | Finite and greater than zero |
| `standardCapacity` | boolean | no | Defaults to `false`; `true` uses the company setting |

```json
{ "firstName": "Alice", "lastName": "Worker", "dailyCapacity": 6.0, "standardCapacity": false }
```

For standard capacity, send `standardCapacity: true`; `dailyCapacity` can be omitted and is ignored.
The company setting defaults to 7.5 hours/day. Employees using it reflect subsequent setting changes.
See [company capacities](company-capacities.md).

**Success response:** `201 Created` with an Employee object. The employee starts active, with no competencies.
Configure competencies using the separate endpoint below.
**Errors:** `400` for invalid names, missing custom capacity, or non-positive/non-finite custom capacity.

## GET /employee

**Success response:** `200 OK` with Employee objects, including inactive employees.

## GET /employee/{id}

**Path parameters:** `id` is a positive employee ID.
**Success response:** `200 OK` with an Employee object.
**Errors:** `400` for an invalid ID; `404` when the employee does not exist.

## PUT /employee/{id}

**Path parameters:** `id` is a positive employee ID.
**Request body:** Names are required, with the same rules as registration.
Omitting `standardCapacity` preserves the existing capacity mode. Custom mode requires `dailyCapacity`.
This operation preserves competencies and active status.
**Success response:** `200 OK` with an Employee object.
**Errors:** `400` for invalid fields or ID; `404` when the employee does not exist.

## PUT /employee/{id}/competences

**Path parameters:** `id` is a positive employee ID.

| Field | Type | Required | Rules |
|---|---|---|---|
| `competenceIds` | number[] | yes | Existing positive IDs; duplicate IDs are counted once |

```json
{ "competenceIds": [1, 2] }
```

This replaces the whole selection. Send `[]` to clear it. New associations require active competencies;
already-associated inactive competencies can be retained or removed. All IDs are validated before saving,
so an invalid selection leaves the existing associations intact.

**Success response:** `200 OK` with the updated Employee object.
**Errors:** `400` for a missing list, invalid IDs, or a newly associated inactive competency;
`404` when an employee or competency does not exist.

## GET /employee/capacity

| Query parameter | Type | Required | Rules |
|---|---|---|---|
| `competenceId` | number | no | Existing positive competency ID |

**Success response:** `200 OK`:

```json
{ "competenceId": null, "employeeCount": 2, "dailyCapacity": 9.0 }
```

Capacity is the sum of effective daily hours of active employees, before task assignments.
Without a filter, every active employee contributes once, including employees without competencies.
With a filter, only active employees holding that competency contribute, once per employee.
Existing inactive competency labels can still be used as a filter.
An empty matching group returns zero employees and zero hours.

Competency totals overlap when employees have multiple competencies: do not add those totals to calculate
company capacity. Use the unfiltered result instead. This endpoint does not subtract assignments, account
for absence, calculate periods, change task duration, or detect over-allocation.

**Errors:** `400` for an invalid filter; `404` when the competency does not exist.

## PATCH /employee/{id}/activate and /employee/{id}/deactivate

**Path parameters:** `id` is a positive employee ID.
**Success response:** `204 No Content`.
Deactivation excludes the employee from daily capacity without deleting the employee or competency associations.
**Errors:** `400` for an invalid ID.

## DELETE /employee/{id}

**Path parameters:** `id` is a positive employee ID.
**Success response:** `204 No Content`.
**Errors:** `400` for an invalid ID; `404` when the employee does not exist.