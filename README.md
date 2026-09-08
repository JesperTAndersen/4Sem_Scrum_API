# Project Estimation & Resource Planner

A web application for estimating projects and planning available resource capacity.

The application is designed for a project/resource manager to create projects with deadlines, divide them into dependent subprojects, estimate required manpower, and determine whether a project is feasible within the available time and quarterly resource capacity.

## Core Features

- Create and manage projects and subprojects
- Plan backwards from a project deadline
- Define dependencies between subprojects
- Calculate minimum project duration using working days
- Manage employees and competency labels
- Track available resource capacity per quarter
- Compare project requirements with available capacity
- Flag scheduling and resource-capacity conflicts
- Estimate project labor costs using hourly rates per competency
- View resource usage both by project and across multiple projects
- Simple user authentication

## Planning Model

The planner evaluates a project from three perspectives:

1. **Schedule** — Can the dependency chain be completed before the deadline?
2. **Capacity** — Are enough resource hours available for the required competencies?
3. **Cost** — What is the estimated labor cost of the project?

Projects that are not feasible are still allowed in the system but are clearly flagged, allowing the manager to adjust the deadline or project plan.