# Users

Users have a profile, credentials, and a role. All user endpoints require a valid JWT. Profile, email, and password operations are restricted by the service to the authenticated user's own ID; role changes currently have no additional service-level role restriction.

**Base path:** `/api/v1/users`  
**Access:** Authenticated

| Method | Path | Summary |
|---|---|---|
| [`GET`](#get-users) | `/users` | List users |
| [`GET`](#get-usersme) | `/users/me` | Get the current user |
| [`GET`](#get-usersid) | `/users/{id}` | Get a user |
| [`PUT`](#put-usersid) | `/users/{id}` | Update a profile |
| [`PATCH`](#patch-usersidemail) | `/users/{id}/email` | Change an email |
| [`PATCH`](#patch-usersidpassword) | `/users/{id}/password` | Change a password |
| [`PATCH`](#patch-usersidrole) | `/users/{id}/role` | Change a role |
| [`DELETE`](#delete-usersid) | `/users/{id}` | Delete a user |

## User object

| Field | Type | Description |
|---|---|---|
| `id` | number | Unique identifier |
| `firstName`, `lastName` | string | User name |
| `email` | string | Unique email address |
| `role` | enum | `EMPLOYEE` or `PROJECT_MANAGER` |
| `createdAt` | date-time | Account creation time |

```json
{ "id": 1, "firstName": "Ada", "lastName": "Lovelace", "email": "ada@example.com", "role": "EMPLOYEE", "createdAt": "2026-09-18 14:30" }
```

## User reference

Project audit fields use this smaller representation:

```json
{ "id": 1, "firstName": "Ada", "lastName": "Lovelace" }
```

## GET /users

**Success response:** `200 OK` with `User[]`.

## GET /users/me

Returns the user identified by the supplied bearer token.

**Success response:** `200 OK` with a User object.

## GET /users/{id}

**Path parameters:** `id` — user ID.  
**Success response:** `200 OK` with a User object.  
**Errors:** `400` for an invalid ID; `404` when the user does not exist.

## PUT /users/{id}

Updates the profile of the authenticated user matching `{id}`.

**Path parameters:** `id` — user ID.

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `firstName` | string | yes | At least 2 characters |
| `lastName` | string | yes | At least 2 characters |

```json
{ "firstName": "Ada", "lastName": "Byron" }
```

**Success response:** `200 OK` with a User object.  
**Errors:** `400` for invalid input; `403` when changing another user; `404` when the user does not exist.

## PATCH /users/{id}/email

Changes the authenticated user's email.

**Path parameters:** `id` — user ID.

**Request body**

```json
{ "email": "ada.byron@example.com" }
```

`email` is required, must be valid, and must be unique.

**Success response:** `200 OK` with a User object.  
**Errors:** `400` for invalid input; `403` when changing another user; `404` when the user does not exist; `409` for a duplicate email.

## PATCH /users/{id}/password

Changes the authenticated user's password.

**Path parameters:** `id` — user ID.

**Request body**

```json
{ "currentPassword": "Password1!", "newPassword": "NewPassword1!" }
```

Both fields are required. `newPassword` must be at least 8 characters and contain an uppercase letter and a number.

**Success response:** `200 OK` with a User object.  
**Errors:** `400` for invalid input or a wrong current password; `403` when changing another user; `404` when the user does not exist.

## PATCH /users/{id}/role

Changes a user's role.

**Path parameters:** `id` — user ID.

**Request body**

```json
{ "role": "PROJECT_MANAGER" }
```

`role` is required and must be `EMPLOYEE` or `PROJECT_MANAGER`.

**Success response:** `200 OK` with a User object.  
**Errors:** `400` for invalid input; `404` when the user does not exist.

## DELETE /users/{id}

Deletes a user other than the authenticated user. The service rejects attempts to delete your own account.

**Path parameters:** `id` — user ID.  
**Success response:** `204 No Content`.  
**Errors:** `400` for an invalid ID; `404` when the user does not exist.
