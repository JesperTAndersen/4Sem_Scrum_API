# Health

The health endpoint checks whether the application can query its database.

**Base path:** `/api/v1/health`  
**Access:** `ANYONE`

| Method | Path | Summary |
|---|---|---|
| [`GET`](#get-health) | `/health` | Check database availability |

## GET /health

**Success response:** `200 OK`

```json
{ "status": "ok" }
```

**Unavailable response:** `503 Service Unavailable`

```json
{ "status": "unavailable" }
```
