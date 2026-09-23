# Estimo API

The Estimo API is the backend contract used by the Estimo frontend to manage users, projects, stages, tasks, and competences.

| Item | Value |
|---|---|
| Local server | `http://localhost:7070` |
| Base URL | `http://localhost:7070/api/v1` |
| Content type | `application/json` |
| Dates / timestamps | `YYYY-MM-DD` / ISO-8601 date-time |

## Conventions

- Paths use lowercase plural resource names; JSON fields use camelCase.
- IDs are positive numbers. A non-numeric path ID returns `400`.
- Request bodies and responses are JSON unless an endpoint explicitly has no body.
- Enum values are uppercase with underscores.

## Authentication and roles

Send a bearer token with every protected request:

```http
Authorization: Bearer <token>
```

| Access | Meaning |
|---|---|
| `ANYONE` | No token is required. |
| Authenticated | A valid JWT is required; the route has no role restriction. |
| `PROJECT_MANAGER` | A valid JWT with the `PROJECT_MANAGER` role is required. |

The available roles are `PROJECT_MANAGER` and `EMPLOYEE`. Every protected endpoint can return `401` for a missing, malformed, invalid, or expired token. Role-restricted endpoints can additionally return `403`.

## Errors

The application returns errors in one standard shape:

```json
{
    "status": 404,
        "message": "Project not found with id: 42",
        "path": "/api/v1/projects/42",
        "errorId": "3f6c1a52-0000-0000-0000-000000000000",
        "timestamp": "2026-09-21T10:15:30Z"
}
```

Client errors contain a message suitable for display. Server errors use `Internal server error`; include the `errorId` when reporting them. Resource documents list endpoint-specific errors.

## Resources

| Resource | Base path | Description | Document |
|---|---|---|---|
| Authentication | `/auth` | Register and log in | [auth.md](api/auth.md) |
| Users | `/users` | User profiles and credentials | [users.md](api/users.md) |
| Projects | `/projects` | Projects and their hierarchy | [projects.md](api/projects.md) |
| Stages | `/stages` | Stages within projects | [stages.md](api/stages.md) |
| Tasks | `/tasks` | Estimated work and required competences | [tasks.md](api/tasks.md) |
| Competences | `/competences` | Skills and hourly rates | [competences.md](api/competences.md) |
| Health | `/health` | Database readiness check | [health.md](api/health.md) |

The public welcome endpoint is `GET /` (outside the API base URL).