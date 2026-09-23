# Definition of Done

A user story is considered done when:

## Functionality
- Core functionality is implemented according to the acceptance criteria
- Input validation and error handling are implemented, and errors return the project's standard error response
- Authorization rules are enforced where applicable
- The feature is integrated into the application without breaking existing functionality

## Quality
- The feature is covered by relevant tests (happy path, validation failures, and authorization where applicable)
- All tests pass locally and in CI
- Code is self-describing where possible, with comments added for complex domain logic or non-obvious implementation choices
- No `TODO` comments are left in the code; follow-up work is tracked as a GitHub issue in the backlog, and lasting decisions or limitations go in the documentation

## Documentation
- New or changed endpoints are documented following the template in [coding standards](coding-standards.md)

## Delivery
- Changes are reviewed and merged to the main branch via pull request
