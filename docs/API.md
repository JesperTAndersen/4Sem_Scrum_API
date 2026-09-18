# Estimo API reference

Frontend reference for the API currently implemented in this repository.

| Item | Value |
|---|---|
| Local server | `http://localhost:7070` |
| API base URL | `http://localhost:7070/api/v1` |
| Content type | `application/json` |
| Dates | `YYYY-MM-DD` |
| Timestamps | ISO-8601 date-time strings |

## Authentication and authorization

Send this header to every protected endpoint:

```http
Authorization: Bearer <token>
```

| Access label | Current behavior |
|---|---|
| ANYONE | No token required. |
| PROJECT_MANAGER | Valid JWT with `PROJECT_MANAGER` role required. |
| Authenticated | Valid JWT required; no route-level role restriction currently exists. |

Valid roles: `PROJECT_MANAGER`, `EMPLOYEE`.

## Endpoint overview

### Authentication

| Method | Path | Access | Request body | Success response |
|---|---|---|---|---|
| `POST` | `/auth/register` | ANYONE | [Register request](#register-request) | `201` [Authenticated user](#authenticated-user) |
| `POST` | `/auth/login` | ANYONE | [Login request](#login-request) | `200` [Login response](#login-response) |

### Projects

All project endpoints require `PROJECT_MANAGER`.

| Method | Path | Request body | Success response | Notes |
|---|---|---|---|---|
| `GET` | `/projects` | — | `200` `ProjectSlim[]` | Returns project summaries and task counts. |
| `GET` | `/projects/{id}` | — | `200` `Project` | Returns the `Project → Stage → Task` hierarchy. |
| `POST` | `/projects` | [Create project](#create-project-request) | `201` `Project` | New projects have `DRAFT` status. |
| `PUT` | `/projects/{id}` | [Update project](#update-project-request) | `200` `Project` | `status` is required. |
| `DELETE` | `/projects/{id}` | — | `204` no body | — |

### Stages

All stage endpoints require `PROJECT_MANAGER`.

| Method | Path | Request body | Success response | Notes |
|---|---|---|---|---|
| `GET` | `/stages` | — | `200` `Stage[]` | Each response always contains `tasks`. |
| `GET` | `/stages/{id}` | — | `200` `Stage` | — |
| `POST` | `/stages` | [Create stage](#create-stage-request) | `201` `Stage` | The referenced project must exist. |
| `PUT` | `/stages/{id}` | [Update stage](#update-stage-request) | `200` `Stage` | Edits only stage-owned fields. |
| `DELETE` | `/stages/{id}` | — | `204` no body | — |

### Tasks

Task endpoints currently require authentication, but do not yet have a
route-level manager restriction.

| Method | Path | Request body | Success response | Notes |
|---|---|---|---|---|
| `GET` | `/tasks` | — | `200` `Task[]` | — |
| `GET` | `/tasks/{id}` | — | `200` `Task` | — |
| `POST` | `/tasks` | [Create task](#create-task-request) | `201` `Task` | The referenced stage must exist. |
| `PUT` | `/tasks/{id}` | — | — | Route exists but is **not implemented**; do not use it yet. |
| `DELETE` | `/tasks/{id}` | — | `200` no body | — |

### Competencies

Competency endpoints currently require authentication, but do not yet have a
route-level manager restriction.

| Method | Path | Request body | Success response |
|---|---|---|---|
| `GET` | `/competences` | — | `200` `Competence[]` |
| `GET` | `/competences/{id}` | — | `200` `Competence` |
| `POST` | `/competences` | [Competence request](#competence-request) | `201` `Competence` |
| `PUT` | `/competences/{id}` | [Competence request](#competence-request) | `200` `Competence` |
| `DELETE` | `/competences/{id}` | — | `204` no body |

`rate` must be a JSON number, not a numeric string.

### Users

User endpoints currently require authentication. Profile operations are limited
by the service to the user whose ID is in the path. Role changes currently have
no additional service-level authorization check.

| Method | Path | Request body | Success response | Notes |
|---|---|---|---|---|
| `GET` | `/users` | — | `200` `User[]` | — |
| `GET` | `/users/me` | — | `200` `User` | Current authenticated user. |
| `GET` | `/users/{id}` | — | `200` `User` | — |
| `PUT` | `/users/{id}` | [Update user](#update-user-request) | `200` `User` | — |
| `PATCH` | `/users/{id}/email` | [Change email](#change-email-request) | `200` `User` | — |
| `PATCH` | `/users/{id}/password` | [Change password](#change-password-request) | `200` `User` | — |
| `PATCH` | `/users/{id}/role` | [Change role](#change-role-request) | `200` `User` | — |
| `DELETE` | `/users/{id}` | — | `204` no body | — |

### Public utility endpoints

| Method | Path | Access | Success response |
|---|---|---|---|
| `GET` | `/` | ANYONE | `200` welcome message |
| `GET` | `/health` | ANYONE | `200` when database is reachable; `503` otherwise |

`/` is outside the API base URL. `/health` is under `/api/v1`.

## Request schemas

### Register request

```json
{
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada@example.com",
  "password": "Password1"
}
```

### Login request

```json
{
  "email": "ada@example.com",
  "password": "Password1"
}
```

### Create project request

```json
{
  "title": "New project",
  "description": "Optional project description",
  "startDate": "2030-01-01",
  "deadline": "2030-04-01"
}
```

### Update project request

```json
{
  "title": "Updated project",
  "description": "Updated description",
  "startDate": "2030-02-01",
  "deadline": "2030-06-01",
  "status": "PLANNED"
}
```

Project statuses: `DRAFT`, `PLANNED`, `IN_PROGRESS`, `COMPLETED`.

### Create stage request

```json
{
  "projectId": 1,
  "name": "Planning"
}
```

### Update stage request

```json
{
  "name": "Updated planning"
}
```

### Create task request

```json
{
  "stageId": 10,
  "name": "Requirements",
  "estimate": 8.0,
  "duration": 8.0,
  "crewSize": 1
}
```

### Competence request

```json
{
  "name": "Backend development",
  "rate": 850.00
}
```

### Update user request

```json
{
  "firstName": "Ada",
  "lastName": "Byron"
}
```

### Change email request

```json
{
  "email": "ada.byron@example.com"
}
```

### Change password request

```json
{
  "currentPassword": "Password1",
  "newPassword": "NewPassword1"
}
```

### Change role request

```json
{
  "role": "PROJECT_MANAGER"
}
```

## Response schemas

### Authenticated user

```json
{
  "id": 1,
  "email": "ada@example.com",
  "role": "EMPLOYEE"
}
```

### Login response

```json
{
  "token": "eyJ..."
}
```

### ProjectSlim

```json
{
  "id": 1,
  "title": "Website redesign",
  "description": "Redesign the public website",
  "createdBy": { "id": 1, "firstName": "Alice", "lastName": "Manager" },
  "startDate": "2026-01-05",
  "deadline": "2026-06-30",
  "taskCountDTO": { "totalTaskCount": 3, "taskFinished": 1 },
  "status": "IN_PROGRESS"
}
```

### Project

```json
{
  "id": 1,
  "title": "Website redesign",
  "description": "Redesign the public website",
  "startDate": "2026-01-05",
  "deadline": "2026-06-30",
  "status": "IN_PROGRESS",
  "createdBy": { "id": 1, "firstName": "Alice", "lastName": "Manager" },
  "createdAt": "2026-01-01T09:00:00",
  "updatedBy": { "id": 1, "firstName": "Alice", "lastName": "Manager" },
  "updatedAt": "2026-01-02T10:00:00",
  "stages": [
    {
      "id": 10,
      "name": "Planning",
      "tasks": [
        {
          "id": 100,
          "name": "Requirements",
          "estimate": 8.0,
          "duration": 8.0,
          "crewSize": 1,
          "status": "NOT_STARTED"
        }
      ]
    }
  ]
}
```

### Stage

```json
{
  "id": 10,
  "name": "Planning",
  "tasks": []
}
```

### Task

```json
{
  "id": 100,
  "name": "Requirements",
  "estimate": 8.0,
  "duration": 8.0,
  "crewSize": 1,
  "status": "NOT_STARTED"
}
```

### Competence

```json
{
  "id": 1,
  "name": "Backend development",
  "rate": 850.00
}
```

### User

```json
{
  "id": 1,
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada@example.com",
  "role": "EMPLOYEE",
  "createdAt": "2026-09-18 14:30"
}
```

### API error

Project and stage validation/not-found errors use this response shape:

```json
{
  "status": 404,
  "message": "Project not found with id: 42"
}
```

Common status codes are `400` (invalid input), `401` (missing/invalid token),
`403` (insufficient role), and `404` (missing resource).
