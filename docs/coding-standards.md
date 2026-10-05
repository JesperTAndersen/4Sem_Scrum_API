# Coding Standards

**Version:** 2.1  
**Date:** 2026-09-30  
**Project:** Scrum Project - ***Estimo***

---

## 1. Overview

These standards keep our code consistent, readable and easy to change. They apply to every team member and to both repositories:

- **Backend:** `4Sem_Scrum_API`
- **Frontend:** `4Sem_Scrum_Frontend`

Three sources work together. Where they overlap, all of them must hold.

| Source | Answers |
|---|---|
| This document | How do we structure, write and deliver code? |
| [Definition of Done](definition-of-done.md) | When is a user story finished? |
| `.editorconfig` | How is code formatted? |

The guiding idea is that good design means **reducing complexity**. If a rule here makes the code more complicated instead of simpler, raise it in the retrospective.

**Changing these standards:** propose the change in a pull request to this file, agree on it in the team, and update the version and date.

---

## 2. Tech Stack

Exact versions live in `pom.xml` (backend) and `package.json` (frontend), not here.

### 2.1 Backend

| Area | Technology |
|---|---|
| Language | Java 25 |
| Web framework | Javalin 7 |
| Persistence | JPA with Hibernate ORM 7, HikariCP connection pool |
| Database | PostgreSQL |
| JSON | Jackson (with `jsr310` for dates and times) |
| Security | JWT (Nimbus JOSE + JWT), password hashing with jBCrypt |
| Boilerplate | Lombok |
| Logging | SLF4J + Logback |
| Build | Maven |
| Testing | JUnit 6, REST Assured, Hamcrest, Testcontainers (PostgreSQL) |

### 2.2 Frontend

| Area | Technology |
|---|---|
| Language | JavaScript (ES modules) |
| UI | React 19, React Router |
| Build | Vite on Node 22 (see `.nvmrc`) |
| Styling | CSS Modules (`*.module.css`), shared variables in `src/styles/variables.css` |
| Code quality | ESLint, Prettier |

### 2.3 Tooling and delivery

| Area | Technology |
|---|---|
| IDE | IntelliJ IDEA |
| Version control | Git and GitHub |
| CI/CD | GitHub Actions: tests run on pull requests to `main`. A push to `main` builds a Docker image and deploys it. |

### 2.4 Dependencies and versions

- Everyone builds with the same Java version (25) and the versions pinned in `pom.xml` / `package.json`.
- Never add or upgrade a dependency without agreeing it with the team.
- Prefer what the project already has over a new dependency. Every dependency is more complexity to carry.

---

## 3. Formatting

The `.editorconfig` in each repository is the **single source of truth** for indentation, line endings, encoding, line length, brace style and import layout. This document does not repeat those rules.

- Use an IDE or editor with EditorConfig support (built into IntelliJ; other editors need the plugin).
- Format the code you change before committing, but do not reformat unrelated code. Reviewable diffs matter.
- Change formatting rules only by changing `.editorconfig`, in a separate pull request.

---

## 4. Naming Conventions

Names describe what something *is* or *does* in domain language, and one concept keeps **one name in every layer** (`Project` → `ProjectDTO` → `/projects`). Avoid abbreviations and vague names such as `data`, `info`, `handle` or `manager`. If a good name is hard to find, the design is probably unclear.

### 4.1 Java (backend)

| Element | Convention | Example |
|---|---|---|
| Package | lowercase, feature first | `app.project.domain` |
| Class, record, enum | PascalCase noun | `ProjectService` |
| Interface | `I` + PascalCase; the implementation drops the `I` | `IProjectService` → `ProjectService` |
| Method | camelCase verb | `getAll()`, `validateCreate()` |
| Variable, field, parameter | camelCase, descriptive | `createdBy`, `startDate` |
| Boolean | `is` / `has` / `can` prefix | `isServerFault` |
| Constant, enum value | UPPER_SNAKE_CASE | `IN_PROGRESS` |
| Test class | `<ClassUnderTest>Test` | `ProjectServiceTest` |

Class suffixes show a class's role:

| Suffix | Role | Example |
|---|---|---|
| `Controller` | Handles HTTP requests | `ProjectController` |
| `Routes` | Registers endpoints and required roles | `ProjectRoutes` |
| `Service` | Business logic | `ProjectService` |
| `DAO` | Database access | `ProjectDAO` |
| `Mapper` | Converts entity ↔ DTO | `ProjectMapper` |
| `DTO` | Request/response record, named `<Action><Entity>DTO` | `CreateProjectDTO`, `ProjectDTO` |
| `Exception` | Error type | `NotFoundException` |
| `Util` | Stateless helper | `RequestUtil` |

### 4.2 JavaScript / React (frontend)

| Element | Convention | Example |
|---|---|---|
| Component file and folder | PascalCase, `.jsx` | `ProjectsTable/ProjectsTable.jsx` |
| Component styles | Same name, `.module.css` | `ProjectsTable.module.css` |
| Route-level component | `Page` suffix | `ProjectDetailPage` |
| Form component | `Form` suffix | `LoginForm` |
| Context | `<Name>Context.jsx` | `AuthContext.jsx` |
| Service | `<feature>Service.js` | `projectService.js` |
| Hook | `use` + PascalCase | `useAuth` |
| Other modules (`.js`) | camelCase | `dateHelpers.js` |
| Variable, function | camelCase | `projectList` |
| Constant | UPPER_SNAKE_CASE | `API_BASE_URL` |
| Feature folder | lowercase | `features/projects/` |

### 4.3 REST API and JSON

- **Paths:** lowercase plural nouns, kebab-case if more than one word (`/projects`, `/stages/{id}`). The HTTP method carries the action, not the path.
- **JSON fields:** camelCase (`firstName`).
- **Enum values:** UPPER_SNAKE_CASE (`PROJECT_MANAGER`).
- **Dates:** `YYYY-MM-DD`. **Timestamps:** ISO-8601.

---

## 5. Architecture

Both repositories follow the same idea, based on [Presentation Domain Data Layering](https://martinfowler.com/bliki/PresentationDomainDataLayering.html): split the code into **presentation**, **domain** and **data**, and organise it by **feature first**, with the layers inside each feature.

### 5.1 Backend

```
app
├── <feature>            project, stage, task, competence, user, security, ...
│   ├── presentation     Controller, Routes, dto/
│   ├── domain           entity, service interface and implementation
│   └── data             DAO interface and implementation, Mapper
├── shared               generic CRUD abstractions used by several features
├── config               application, Hibernate and dependency wiring
├── utils                stateless helpers
└── exceptions           ApiException hierarchy and ErrorResponse
```

| Layer | Responsible for | Must not |
|---|---|---|
| **Presentation** | Reading the request, validating its shape, calling **one service method**, writing the response and status code | Contain business rules or touch persistence |
| **Domain** | Business rules, validation, authorization decisions, orchestration | Know about HTTP or JPA details |
| **Data** | Persistence with JPA/Hibernate, entity ↔ DTO mapping, translating persistence errors | Contain business rules |

Dependencies point one way: presentation → domain → data.

Rules that follow from this:

1. **Depend on interfaces and inject through constructors.** Classes receive their collaborators, including the `EntityManagerFactory`, and never create them. Wiring happens only in `app.config` (`DependencyContainer`, `ApplicationConfig`). This is what lets tests swap in in-memory DAOs or a test database.
2. **Entities never cross the service boundary.** Services return DTOs and controllers only see DTOs. The API contract is the DTOs, so an entity can change without breaking the API.
3. **Access control is declared per route** in `<Feature>Routes` with a `Role`. Decisions that depend on data (for example "only your own profile") belong in the service.
4. **The `EntityManager` never leaves the DAO.** Open it in a try-with-resources block, and bind query values with parameters (`setParameter`), never string concatenation.
5. **Share only what is shared.** Code goes in `app.shared` only when at least two features use it.
6. **A new feature copies the shape of an existing one** (for example `project`), then is registered in `Routes`/`ApplicationConfig` and wired in `DependencyContainer`.

### 5.2 Frontend

Same principles, expressed in React:

```
src/
├── api/           apiclient.js: the only place that talks HTTP
├── context/       cross-cutting state (auth, notifications)
├── features/
│   └── <feature>/ components/, pages/, services/, utils/
├── layouts/       page shells
├── pages/         app-wide pages (not found, forbidden)
├── shared/        reusable UI and layout components
├── styles/        global styles and variables
└── utils/         general helpers
```

- **Components render; services fetch.** Components never call `fetch`. They use their feature's `services/<feature>Service.js`, which uses `apiclient.js`.
- **Features do not reach into each other.** Anything used by more than one feature moves to `shared/` or `utils/`.
- Use absolute imports with the `@/` prefix.
- The API documentation ([section 9](#9-api-documentation)) is the contract between the repositories. A contract change updates the documentation and the frontend together.

---

## 6. Comments

- Code shows *what* and *how*. Comments say what code cannot: **why**, assumptions, constraints, domain rules and non-obvious choices.
- Do not repeat the code. If a comment restates the next line, delete it or improve the name.
- For an interface method whose behaviour is not obvious from its signature (edge cases, exceptions, side effects), write a short Javadoc describing the contract: what callers can rely on, not how it is done.
- Update comments in the same commit as the code they describe.
- No commented-out code. Git remembers it.
- No `TODO` or `FIXME`, as required by the [Definition of Done](definition-of-done.md). Create a GitHub issue instead, and put lasting decisions or limitations in the documentation.

---

## 7. Exception Handling

Expected failures are `ApiException` subclasses, thrown in the layer that detects them. **One** central handler turns them into HTTP responses.

### 7.1 Backend

| Where | Does |
|---|---|
| **Data** (DAO) | Catches JPA `PersistenceException` and throws `DatabaseException` with the cause. A missing entity becomes `NotFoundException`. |
| **Domain** (service) | Enforces business rules and throws `BadRequestException`, `NotFoundException`, `ForbiddenException`, `ConflictException` or `UnauthorizedException`. |
| **Presentation** (controller) | Validates the request shape (`bodyValidator`, `RequestUtil.requirePathId`). Never builds error responses. |
| **`ApplicationConfig.configureExceptions`** | The only place that maps exceptions to a status code and an `ErrorResponse`, and logs them. |

| Exception | Status | Use when |
|---|---|---|
| `BadRequestException` | 400 | Input is well-formed but breaks a rule |
| `UnauthorizedException` | 401 | Credentials or token are missing or invalid |
| `ForbiddenException` | 403 | The user is authenticated but not allowed |
| `NotFoundException` | 404 | The resource does not exist |
| `ConflictException` | 409 | The request conflicts with current state |
| `DatabaseException` | 500 | A persistence operation failed |
| `TokenCreationException` | 500 | A JWT could not be created |
| `ConfigurationException` | 500 | Configuration is missing or invalid. It extends `RuntimeException`, not `ApiException`, and should fail fast at startup. |

Rules:

- All exceptions are unchecked. Anything that must reach the client extends `ApiException`.
- **Keep the cause:** `new DatabaseException("Failed to fetch project by id: " + id, e)`.
- **Never swallow an exception.** No empty `catch` blocks. Catch only to translate, recover or clean up. Otherwise let it propagate.
- **Log once, at the boundary.** Do not log and rethrow. The central handler logs 4xx as warnings and 5xx as errors with the stack trace.
- **4xx messages are for the client.** They say what is wrong and are safe to show (`Project not found with id: 42`).
- **5xx responses are always generic** (`Internal server error`) and carry an `errorId`. The details stay in the log, where the same `errorId` finds them. Never put secrets, SQL or stack traces in a message.
- **Add a new exception class only when it needs its own status code.** Do not create one per entity. `NotFoundException` plus a message is enough. See [7.2](#72-adding-a-new-exception).
- Every error response has the same `ErrorResponse` shape, documented once in `API.md`.

### 7.2 Adding a new exception

When an error needs a status code that no existing exception covers:

1. Look the status up in the [MDN HTTP status reference](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status).
2. **Name the exception after the status name on MDN**, in PascalCase with the `Exception` suffix. Do not invent your own name.

   | MDN status | Exception |
   |---|---|
   | `404 Not Found` | `NotFoundException` |
   | `409 Conflict` | `ConflictException` |
   | `422 Unprocessable Content` | `UnprocessableContentException` |
   | `429 Too Many Requests` | `TooManyRequestsException` |

   The generic `500` is the one exception to this rule. Those failures are named after what failed (`DatabaseException`, `TokenCreationException`).
3. Create it in `app.exceptions`, extending `ApiException`.
4. Pass the status through `HttpStatus.<STATUS>.getCode()` (Javalin's `io.javalin.http.HttpStatus`), never a number literal.
5. Provide two constructors: one with a message, one with a message and a cause.
6. Add it to the table above, and document it in the API docs of the endpoints that can return it.

Example for `422 Unprocessable Content`:

```java
package app.exceptions;

import io.javalin.http.HttpStatus;

public class UnprocessableContentException extends ApiException
{
    public UnprocessableContentException(String message)
    {
        super(HttpStatus.UNPROCESSABLE_CONTENT.getCode(), message);
    }

    public UnprocessableContentException(String message, Throwable cause)
    {
        super(HttpStatus.UNPROCESSABLE_CONTENT.getCode(), message, cause);
    }
}
```

No other code is needed. `ApplicationConfig.configureExceptions` already handles every `ApiException`.

### 7.3 Frontend

- `src/api/apiclient.js` is the only place that interprets error responses. It throws an `Error` carrying `message`, `statusCode` and `data`.
- Branch on `statusCode`, never on the text of `message`.
- 401 and 403 are signalled globally (`auth:unauthorized`, `auth:forbidden`), so components do not handle them one by one.
- Show users a friendly message, not raw technical details. Include the `errorId` from the response in bug reports so the backend log entry can be found.

---

## 8. Testing

Every change ships with tests. The [Definition of Done](definition-of-done.md) sets the scope: **happy path, validation failures, and authorization where applicable**. Section 8.2 defines *how* the test cases for that scope are chosen.

### 8.1 General rules

- Test at the level where the behaviour lives: business rules in service tests, persistence against a real PostgreSQL, and endpoints through HTTP (see [8.4](#84-techniques-per-test-level)).
- Tests use injected dependencies (in-memory DAOs, a test `EntityManagerFactory`). They never touch a production database.
- Tests mirror the main package structure under `src/test/java`.
- Describe behaviour, not implementation: `@DisplayName("Create - should reject a deadline before the project start date")`.
- Tests are independent of each other and of execution order.
- All tests pass locally (`mvn test`) before you open a pull request, and in CI.

### 8.2 Test case design

Test cases are chosen **systematically** with three black-box techniques. They are derived from the acceptance criteria and the documented API rules (`docs/api`), **not from the implementation**. The expected result of a test case comes from the specification. If the code and the specification disagree, the test follows the specification and the disagreement is raised with the team.

```
Acceptance criteria → test design (EP, BVA, decision table) → consolidated test cases → CSV test data → automated test
```

| Technique | Use when | Produces |
|---|---|---|
| Equivalence Partitioning (EP) | An input has groups of values that the system treats the same way | One representative per class, both valid and invalid classes |
| Boundary Value Analysis (BVA) | A class has an edge: a minimum, a maximum, or an ordering between two values | The value on each boundary and the nearest value on each side |
| Decision table | The outcome depends on a combination of two or more conditions | One test case per rule in the table |

#### Equivalence Partitioning

- Split every input into classes where all values should give the same result. Always include the **invalid** classes.
- Pick one representative per class from the **middle** of the class. Boundaries are covered by BVA.
- Start from these standard classes and add the ones the domain rules introduce:

| Input type | Classes to consider |
|---|---|
| Required string | `null`, empty (`""`), whitespace only, valid. Too long, if a maximum length exists |
| Number with a range | Below minimum, within range, above maximum |
| Date pair (e.g. `startDate`, `deadline`) | Second date before the first, second date not before the first |
| Enum | Each value that a rule treats differently. An unknown value at endpoint level |
| Path ID | Non-numeric, zero or negative, positive but unknown, existing |
| Boolean or binary state | Both values |

#### Boundary Value Analysis

- For every boundary, test the value **on** the boundary and the nearest value **on each side**, using the smallest step of the type:

| Type | Step | Example |
|---|---|---|
| Integer | `1` | Range 1–8: `0, 1, 2, 7, 8, 9` |
| Decimal (hours, rates) | Smallest meaningful unit, e.g. `0.01` | Minimum 0 exclusive: `0.00, 0.01` |
| Date | 1 day | `deadline` not before `startDate`: `startDate − 1`, `startDate`, `startDate + 1` |
| String length | 1 character | Max 100: lengths `99, 100, 101` |

- A range open at one end has one boundary. `>= 0` gives `-1, 0, 1`.
- A point **inside** the valid range where a rule changes behaviour is also a boundary. Test both sides of it.

#### Decision tables

- Use a decision table when **two or more conditions together** decide the outcome. Typical cases in Estimo are access control (token, role, ownership) and rules that combine state and input (for example, which changes are allowed in a given status).
- Conditions are columns, the result is the last column, and `-` means *don't care*: the condition does not affect the result in that rule. Combine rules with the same result where a condition does not matter.
- Each rule becomes at least one test case.

Example: `GET /projects/{id}`, restricted to `PROJECT_MANAGER`:

| Rule | Valid token? | `PROJECT_MANAGER`? | Project exists? | Result |
|---|---|---|---|---|
| R1 | No | - | - | `401` |
| R2 | Yes | No | - | `403` |
| R3 | Yes | Yes | No | `404` |
| R4 | Yes | Yes | Yes | `200` |

### 8.3 Consolidation and implementation

The techniques overlap. For example, `0` is both an invalid EP class and a boundary value. The techniques are used to **find** test cases, not to create three separate test suites.

- Merge the test cases from all techniques into **one consolidated set per rule set**, such as the date rules of `create` in `ProjectService`.
- Every EP class, every boundary value and every decision table rule appears in **at least one** row. Remove rows that cover nothing new.
- Each row has a `reason` that names every technique it represents and what it tests.
- Implement the set as a data-driven `@ParameterizedTest` with `@CsvFileSource`.

#### Test data files

Test cases are stored in CSV files, not inline in the test class. This keeps the test design readable and reviewable as a table, separate from the code that runs it. Use a CSV file even for small sets, so all test data is found in the same way.

**Storage**

```
src/test/resources/
└── testcases/
    └── <feature>/                     project, stage, task, competence, user, security, ...
        └── <class-under-test>/        kebab-case, e.g. project-service, project-controller
            └── <rule-set>.csv         kebab-case, e.g. create-dates.csv
```

Example: the test `createValidatesTitleAndDates` in `ProjectServiceTest` reads
`src/test/resources/testcases/project/project-service/create-dates.csv`.

- **One file per parameterized test method, and one method per file.** A file is never shared between methods.
- The rule-set name says what is tested, starting with the method or endpoint action: `create-dates`, `update-status-transitions`, `get-by-id-access`.
- Reference the file with an absolute classpath path, starting with `/` and without `src/test/resources`: `/testcases/project/project-service/create-dates.csv`.
- CSV files are committed, reviewed and changed in the same pull request as the tests that use them.
- Encoding (UTF-8) and line endings follow `.editorconfig`.

**Format**

| Item | Convention |
|---|---|
| Header | First line, camelCase names matching the method parameters, skipped with `numLinesToSkip = 1` |
| Column order | Inputs, then `expected`, then `reason` |
| `expected` | Service tests: `ACCEPT` or the exception's simple class name (`BadRequestException`). Endpoint tests: the HTTP status code (`201`, `400`) |
| `reason` | `<techniques>: <what the row tests>`, in English, e.g. `"EP + BVA: deadline one day before startDate"` |
| Quoting | Quote values that contain a comma, or leading or trailing spaces. Always quote `reason` |
| `null` / empty | `NULL` with `nullValues = "NULL"`. Empty string is `""` |
| Dates | `YYYY-MM-DD`, fixed values. Never relative to today |
| Display name | `name = "[{index}] {n}"`, where `n` is the zero-based index of `reason` |

```
title,startDate,deadline,expected,reason
Website redesign,2030-01-01,2030-04-01,ACCEPT,"EP: valid title and dates"
NULL,2030-01-01,2030-04-01,BadRequestException,"EP: title is null"
"   ",2030-01-01,2030-04-01,BadRequestException,"EP: title is whitespace only"
Website redesign,2030-01-01,2029-12-31,BadRequestException,"EP + BVA: deadline one day before startDate"
Website redesign,2030-01-01,2030-01-01,ACCEPT,"BVA: deadline equal to startDate"
Website redesign,2030-01-01,2030-01-02,ACCEPT,"BVA: deadline one day after startDate"
```

```java
@ParameterizedTest(name = "[{index}] {4}")
@CsvFileSource(resources = "/testcases/project/project-service/create-dates.csv", numLinesToSkip = 1, nullValues = "NULL")
@DisplayName("Create - validates title and dates")
void createValidatesTitleAndDates(String title, LocalDate startDate, LocalDate deadline, String expected, String reason)
{
    CreateProjectDTO dto = new CreateProjectDTO(title, startDate, deadline);

    if (expected.equals("ACCEPT"))
    {
        ProjectDTO result = projectService.create(dto);
        assertThat(result.title(), is(title));
    }
    else
    {
        ApiException exception = assertThrows(ApiException.class, () -> projectService.create(dto));
        assertThat(exception.getClass().getSimpleName(), is(expected));
    }
}
```

Tests that do not check an input or business rule, such as correct mapping or a single happy path through HTTP, stay ordinary `@Test` methods with a `@DisplayName`.

### 8.4 Techniques per test level

| Level | Tools | Apply |
|---|---|---|
| Service (in-memory DAOs) | JUnit, Hamcrest | **The main place.** EP, BVA and decision tables for business rules, validation and data-dependent authorization ("only your own profile") |
| Endpoint (REST Assured, Testcontainers) | REST Assured, Hamcrest | EP for request shape (missing body, non-numeric ID), a decision table for route access (`401` / `403` / `404` / `2xx`), one representative invalid case per rule set to prove the error mapping. Do **not** repeat every boundary through HTTP |
| DAO (Testcontainers) | JUnit, Hamcrest | Normally none. Test persistence behaviour: saved, found, not found, constraint violations |

### 8.5 Designing test cases step by step

Follow these steps whenever tests are written or changed:

1. **List the rules** for the service method or endpoint: the acceptance criteria, the *Rules* column of the request body in `docs/api`, the role in `<Feature>Routes`, and data-dependent authorization.
2. **Partition** every input with EP (8.2).
3. **Find the boundaries** of every class with BVA (8.2).
4. **Build a decision table** where two or more conditions combine (8.2).
5. **Consolidate** into one CSV per rule set, with `expected` and `reason` (8.3).
6. **Implement** the rule set as one `@ParameterizedTest` that reads its CSV file (see [Test data files](#test-data-files)). A test case covered by a CSV row is not repeated as a separate `@Test`.
7. **Treat a failing row as a bug or an unclear rule.** Never change `expected` to match the code. Fix the code, or raise the rule with the team (8.2).

### 8.6 Frontend

There is no test framework yet. Until one is adopted, `npm run lint` and `npm run format:check` must pass and the change is verified manually against the running API.

---

## 9. API Documentation

An endpoint is not done until it is documented (see the [Definition of Done](definition-of-done.md)). Update the documentation **in the same pull request** as the code change.

### 9.1 Structure

```
docs/
├── API.md          overview and table of contents
└── api/
    ├── auth.md
    ├── users.md
    ├── projects.md
    ├── stages.md
    ├── tasks.md
    ├── competences.md
    └── health.md
```

- **`API.md`** holds everything that applies to *every* endpoint, written once: base URL, conventions, authentication and roles, the standard error response, and the table of contents linking to each resource document.
- **`docs/api/<resource>.md`** covers one resource. The file is named after the first path segment of its endpoints (`/projects/...` → `projects.md`). It contains the endpoints **and** their request and response schemas together, so a reader never has to scroll elsewhere.
- **Do not copy types owned by another resource.** Link to them (for example the `createdBy` user reference in `users.md`).
- **Document what the code does**: paths and roles from `*Routes`, fields from the DTOs, status codes from the exceptions. Call the endpoint once to verify the examples.

### 9.2 `API.md` template

````markdown
# Estimo API

One sentence on what the API is and who uses it.

| Item | Value |
|---|---|
| Base URL | `http://localhost:7070/api/v1` |
| Content type | `application/json` |
| Dates / timestamps | `YYYY-MM-DD` / ISO-8601 |

## Authentication and roles

How to send the token (`Authorization: Bearer <token>`), and what each access label means
(`ANYONE`, `EMPLOYEE`, `PROJECT_MANAGER`).

## Errors

Every error uses this shape:

```json
{
  "status": 404,
  "message": "Project not found with id: 42",
  "path": "/api/v1/projects/42",
  "errorId": "3f6c1a52-…",
  "timestamp": "2026-09-21T10:15:30Z"
}
```

Any protected endpoint can also return `401` (missing or invalid token) and `403` (insufficient
role). Resource documents list only the errors that are specific to an endpoint.

## Resources

| Resource | Base path | Description | Document |
|---|---|---|---|
| Projects | `/projects` | Projects and their full hierarchy | [projects.md](api/projects.md) |
| Stages | `/stages` | Stages of a project | [stages.md](api/stages.md) |
````

### 9.3 Resource document template

````markdown
# Projects

One or two sentences: what this resource is and how it relates to others.

**Base path:** `/api/v1/projects`
**Access:** `PROJECT_MANAGER` (unless an endpoint says otherwise)

| Method | Path | Summary |
|---|---|---|
| [`GET`](#get-projects) | `/projects` | List project summaries |
| [`POST`](#post-projects) | `/projects` | Create a project |

## Project object

| Field | Type | Description |
|---|---|---|
| `id` | number | Unique identifier |
| `title` | string | Project title |
| `status` | enum | `DRAFT`, `PLANNED`, `IN_PROGRESS`, `COMPLETED` |

```json
{ "id": 1, "title": "Website redesign", "status": "DRAFT" }
```

---

## POST /projects

Creates a project. New projects start as `DRAFT`.

**Access:** `PROJECT_MANAGER`

**Request body**

| Field | Type | Required | Rules |
|---|---|---|---|
| `title` | string | yes | Not blank |
| `startDate` | date | yes | `YYYY-MM-DD` |
| `deadline` | date | yes | Not before `startDate` |

```json
{ "title": "New project", "startDate": "2030-01-01", "deadline": "2030-04-01" }
```

**Success response:** `201 Created` with a [Project](#project-object)

**Errors**

| Status | When |
|---|---|
| `400` | `title` is blank, or `deadline` is before `startDate` |
````

Endpoint sections are titled `## <METHOD> <path>`. Include a **Path parameters** table when the path has `{id}` parameters, and a `Notes` line only when behaviour is not obvious.

---

## 10. Git Workflow

This applies to both repositories.

### 10.1 Branches

Branches are named `<type>/<description>`, following [Conventional Branch](https://conventionalbranch.org/). The description is short and specific, lowercase, with words separated by hyphens (`feat/project-crud`, `fix/configuration-exception`).

| Branch | Purpose | Created from | Merged into |
|---|---|---|---|
| `main` | Production code. **A merge deploys.** | | |
| `developer` | Integration branch | | `main`, by pull request |
| `feat/<description>` | New feature or user story | `developer` | `developer` |
| `fix/<description>` | Bug fix | `developer` | `developer` |
| `chore/<description>` | Maintenance that does not change behaviour: build, dependencies, documentation, configuration | `developer` | `developer` |
| `release/<version>` | Prepare a release, e.g. `release/v1.2.0` | `developer` | `main`, then back into `developer` |
| `hotfix/<description>` | Urgent production fix | `main` | `main`, then `developer` |

Use the short forms `feat` and `fix`. Delete a branch after it is merged.

### 10.2 Commits

Commits follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/):

```
<type>[optional scope]: <description>
```

| Type | Use for |
|---|---|
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation only |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `test` | Adding or correcting tests |
| `chore` | Maintenance, build, dependencies, tooling |

Also allowed: `perf`, `style`, `build` and `ci`.

- Write in **English**, imperative mood ("add", not "added"), lowercase, no trailing period, at most 72 characters.
- The scope is the feature or area: `project`, `security`, `exceptions`.
- One logical change per commit. Use the body to explain *why* when it is not obvious.
- Reference an issue in the footer (`Closes #42`). Mark a breaking change with `!` (`feat(api)!: ...`).
- Avoid meaningless messages such as `updates`, `fix` or `asdf`.
- **Frequency:** commit after each logical change and push at least once every working day.

```
feat(project): add endpoint for updating project status
fix(exceptions): make ConfigurationException extend RuntimeException
docs: split API reference into one file per resource
```

### 10.3 Pull requests

- All merges into `developer` and `main` go through a pull request. No direct pushes.
- The title follows the commit format. The description says what changed and why, and links the issue.
- Before opening: `mvn test` passes and every point of the [Definition of Done](definition-of-done.md) is met.
- At least **one other developer** reviews it (see [section 11](#11-definition-of-done-and-review)).
- Keep it small and focused on one story or concern.

### 10.4 Merge conflicts

- Resolve conflicts in **your own branch**: merge the latest `developer` into it, resolve, re-run the tests and push.
- Unsure? Ask the team.

---

## 11. Definition of Done and Review

A user story is done only when **every point** in the [Definition of Done](definition-of-done.md) is met. The author checks it before opening a pull request and the reviewer verifies it.

In addition, the reviewer checks the change against these standards:

- [ ] **Layers respected.** Controllers call services, entities do not leave the service, wiring is in `app.config` (section 5).
- [ ] **Errors handled properly.** `ApiException` subclasses, no swallowed exceptions, causes kept, generic 5xx messages (section 7).
- [ ] **Tests designed systematically.** Test cases are derived with EP, BVA and decision tables, consolidated into CSV files, and every row has a `reason` (section 8).
- [ ] **Names and structure** follow sections 4 and 5. Formatting matches `.editorconfig`.
- [ ] **Comments explain why.** No `TODO`, no commented-out code (section 6).
- [ ] **Endpoint documentation** is updated (section 9).
- [ ] **No secrets** in code, configuration or commits. Local values belong in `.env`, which is never committed.
- [ ] **Branch and commit names** follow section 10.

---

## 12. Roles and Responsibilities

### Developers

- **Self-organise.** Developers pick work from the backlog themselves. Nobody assigns tasks.
- Follow these coding standards and the Definition of Done on every piece of work. A story that does not meet them is not done.
- Review each other's pull requests.
- Keep task status on the board current, so that anyone can continue the work when someone is away.
- Raise problems and blockers early, in the Daily Scrum or the team chat.

### Scrum Master

- A servant-leader for the team. Responsible for the team understanding and applying Scrum, including these standards and the Definition of Done.
- Makes sure the Scrum events (Sprint Planning, Daily Scrum, Sprint Review, Retrospective) take place and are productive.
- Removes impediments, or helps the team remove them, and shields the team from outside disruption.
- Coaches the team in self-management, and brings proposed changes to the standards or the Definition of Done to the retrospective.
- **Does not** assign tasks, decide how the work is done, or act as a line manager. When someone is absent, the team decides how to cover the work. The Scrum Master helps make the situation visible and clears the way.
