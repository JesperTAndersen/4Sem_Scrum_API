# Company capacities

All paths are relative to `/api/v1` and require the `PROJECT_MANAGER` role.
See [authentication and errors](../API.md).

| Method | Path | Description |
|---|---|---|
| GET | `/company-capacities` | Read standard daily capacity |
| PUT | `/company-capacities` | Update standard daily capacity |

## Company capacity object

```json
{ "dailyCapacity": 7.5 }
```

`dailyCapacity` is the standard number of hours per employee per working day, not a company-wide total.
It defaults to 7.5 and is initialized when the application starts.

## GET /company-capacities

**Success response:** `200 OK` with a Company capacity object.

## PUT /company-capacities

| Field | Type | Required | Rules |
|---|---|---|---|
| `dailyCapacity` | number | yes | Finite and greater than zero |

**Success response:** `200 OK` with the updated Company capacity object.
**Errors:** `400` for a missing or invalid capacity.

Employees using standard capacity reflect the current value; employees with custom capacity retain their
own hours. See [employees](employee.md).