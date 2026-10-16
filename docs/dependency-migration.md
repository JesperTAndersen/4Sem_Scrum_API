# Dependency migration and deployment

**Review before deploying against an existing database.**

1. Back up the database. Audit legacy cross-stage task dependencies:

```sql
SELECT d.task_id, t.stage_id AS dependent_stage_id,
       d.predecessor_id, p.stage_id AS predecessor_stage_id
FROM task_predecessors d
JOIN task t ON t.id = d.task_id
JOIN task p ON p.id = d.predecessor_id
WHERE t.stage_id <> p.stage_id;
```

2. Review each row with the team. Decide whether to create a stage dependency (affects **all** tasks in the dependent stage), restructure stages, or remove the old link. **Do not auto-convert.** Delete the obsolete task dependency only after its replacement is approved.
3. Deploy changes and inspect Hibernate's generated `stage_predecessors` join table (`stage_id`, `predecessor_id`). The current application uses Hibernate schema update; for production, prefer an explicit reviewed migration. Existing data is not modified automatically.
4. Validate on a database copy before promoting. Check for tasks already in progress that would violate predecessor completion rules.

## Decisions adopted

- An empty stage is **not completed**, and imposes no calendar delay; an empty predecessor still blocks starting or completing tasks.
- Zero-duration tasks finish on their scheduled start; successors may start on that date.
- Dependencies cannot be added to a task/stage with started work; removing dependencies is allowed.
- Reopening a predecessor is denied when a task or stage dependent on it has started.
- Adding unfinished tasks to a completed stage is denied if dependent-stage work has started.
- Deleting a stage with any dependent stage is denied; explicitly remove incoming dependencies first.
- Existing cross-stage task dependencies require manual review and are **not** migrated implicitly.

## Test and rollout note

Run the complete suite under **Java 25** with `mvn test`. Integration tests require configured PostgreSQL/Testcontainers. Review all changes to the dependency-related fixtures for genuine intended behavior, not just green checks.
