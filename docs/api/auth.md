# Authentication

Authentication creates accounts and exchanges credentials for JWTs.

**Base path:** `/api/v1/auth`  
**Access:** `ANYONE`

| Method | Path | Summary |
|---|---|---|
| [`POST`](#post-authregister) | `/auth/register` | Register a user |
| [`POST`](#post-authlogin) | `/auth/login` | Log in and receive a token |

## POST /auth/register

Creates a user with the default `EMPLOYEE` role.

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `firstName` | string | yes | Not blank |
| `lastName` | string | yes | Not blank |
| `email` | string | yes | Valid email format; unique |
| `password` | string | yes | At least 8 characters with uppercase, lowercase, digit, and special character |

```json
{ "firstName": "Ada", "lastName": "Lovelace", "email": "ada@example.com", "password": "Password1!" }
```

**Success response:** `201 Created`

```json
{ "id": 1, "email": "ada@example.com", "role": "EMPLOYEE" }
```

**Errors:** `400` for invalid input; `409` if the email already exists.

## POST /auth/login

Authenticates an existing user.

**Request body**

| Field | Type | Required |
|---|---|---|
| `email` | string | yes |
| `password` | string | yes |

```json
{ "email": "ada@example.com", "password": "Password1!" }
```

**Success response:** `200 OK`

```json
{ "token": "eyJ..." }
```

**Errors:** `400` for malformed request data; `401` for invalid credentials.
