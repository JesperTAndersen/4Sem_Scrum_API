# Estimo - Project Estimation & Resource Planning

[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=JesperTAndersen_4Sem_Scrum_API&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=JesperTAndersen_4Sem_Scrum_API)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=JesperTAndersen_4Sem_Scrum_API&metric=coverage)](https://sonarcloud.io/summary/new_code?id=JesperTAndersen_4Sem_Scrum_API)
[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=JesperTAndersen_4Sem_Scrum_API&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=JesperTAndersen_4Sem_Scrum_API)

Estimo helps project managers estimate work, plan schedules, and understand whether their team has enough time to deliver a project.

This repository contains the backend API used by the Estimo frontend. It manages project plans, employees, skills, and the calculations behind schedules, workload, and estimated labor costs.

## What Estimo supports

- Organize projects into stages and tasks, with a start date and deadline.
- Estimate task hours using best-case, most likely, and worst-case estimates.
- Define which stages and tasks must finish before other work can begin.
- Assign employees based on their skills and daily working hours.
- Calculate working-day schedules and check whether plans meet their deadlines.
- Find employees with available time and spot daily workload conflicts across projects.
- Estimate labor costs using the hourly rate for each skill.
- Track progress, upcoming deadlines, and project risks through a dashboard overview.
- Control access through user accounts and roles.

## How planning works

A **project** contains **stages**, and each stage contains **tasks**. Tasks describe the work to be done, its estimated hours, the skill it requires, and the employees assigned to it.

Schedules calculate forward from the project start date, using Monday to Friday as working days. The order of work, assigned employees' capacity, and each task's minimum duration determine the planned dates. The calculated finish date is then compared with the project deadline.

Estimo helps managers answer three questions:

- **Schedule:** Can the planned work finish before the deadline?
- **Capacity:** Do employees have enough available hours, including work on other projects?
- **Cost:** What is the estimated labor cost based on the work and required skills?

A plan can still be saved when it exceeds its deadline. Workload conflicts are reported separately so managers can adjust assignments, timing, or scope. These checks highlight problems; they do not automatically resolve conflicting assignments.

When a task has no available, eligible employees, its schedule may remain undetermined until staffing is resolved. Inactive employees and employees who no longer have the required skill do not contribute capacity to unfinished work. Existing assignments remain visible, with warnings where relevant. Completed tasks retain their recorded planning dates and durations when available.

## Running locally

The backend uses Java 25, Maven, Javalin, Hibernate, and PostgreSQL.

1. Install Java 25 and Maven, and create a local PostgreSQL database on port `5432`.
2. Set `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` as environment variables for that database.
3. Set `JWT_ISSUER` and `JWT_SECRET` for authentication. The secret must be at least 32 characters long. See [.env.example](.env.example) for example variable names; configure these in your shell or IDE before running.
4. With Docker running for the database-backed tests, build and check the project, then start the API:

   ```sh
   mvn verify
   java -jar target/app.jar
   ```

The server runs at `http://localhost:7070`, with API endpoints under `/api/v1`.

## Further documentation

- [API overview](docs/API.md) — authentication, roles, endpoints, and request examples.
- [Projects and scheduling](docs/api/projects.md) — project structure and deadline checks.
- [Tasks and staffing](docs/api/tasks.md) — estimates, assignments, warnings, and completed planning records.
- [Employee workload](docs/api/workload.md) — daily availability and conflicts across projects.
- [Coding standards](docs/coding-standards.md) — development conventions and testing guidance.
- [Definition of Done](docs/definition-of-done.md) — requirements for completing a user story.
- [Dependency migration and deployment](docs/dependency-migration.md) — guidance for existing dependency data and rollout.

## Project quality
[![SonarQube Cloud](https://sonarcloud.io/images/project_badges/sonarcloud-dark.svg)](https://sonarcloud.io/summary/new_code?id=JesperTAndersen_4Sem_Scrum_API)