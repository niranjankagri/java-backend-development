# Topic 4: Releasing safely: no downtime, a check after every release, a way back

A deployment replaces a running version with a new one while users keep using it. Every piece built in
Sessions 23 and 24 has a part in doing that without anyone noticing.

## The order of a release

```
commit on main
  └─ GitHub checks: tests (Java 17, 25) + image pipeline with smoke test     topic 1
        └─ all green? Render builds and starts the NEW version               autoDeployTrigger: checksPass
              └─ Flyway migrates the database (if there is a new migration)  Session 23, topic 2
              └─ Hibernate validates entities against the schema             fails here, not under load
              └─ readiness UP?  only now traffic moves to the new version    healthCheckPath
                    └─ OLD version: SIGTERM, finishes its requests (20 s)    graceful shutdown
                          └─ smoke test against the public address           scripts/smoke-test.sh
```

If the new version never becomes ready (it crashes, the migration fails, the schema does not match),
Render keeps the old version serving and marks the deploy as failed. Users see nothing.

## Migrations must work with both versions

For a short time the old and the new version run at the same time, on the same database, and the
migration has already run. So a migration must not break the old version:

| Change | Safe? | How |
|---|---|---|
| add a table, a nullable column, an index | yes | in one release |
| add a NOT NULL column | careful | add it nullable (or with a default), fill it, add NOT NULL in a later release |
| rename or remove a column | not in one step | expand and contract: add the new one, write both, switch reads, remove the old one later |

Migrations only go forward. Flyway Community has no "undo"; a mistake is fixed by the next migration.

## After every release: the smoke test

```bash
airbnb-backend/scripts/smoke-test.sh https://<service-name>.onrender.com
```

It proves, in seconds, that the deployed service is wired up: database, migrations, login, the main path
and the double-booking guard (topic 1). It leaves no visible data behind (the listing is deactivated);
its users stay, with unique `smoke-...@example.com` addresses.

## The way back

| Problem | Way back |
|---|---|
| a bug in the code | Render: **Rollback** to the previous deploy (the previous image), or revert the commit on `main` (it deploys like any other) |
| a bug in a migration | a new migration that repairs it (forward only); the old version still runs on the expanded schema |
| data damaged | the database backups (paid plans; the free plan has none) |

Because images are tagged by commit (topic 2), "what was running before" is always known exactly.

## Interview questions

- **How do you deploy without downtime?** Start the new version next to the old one, switch traffic
  when it is ready (readiness), stop the old one gracefully.
- **Why must migrations be backwards compatible?** Old and new code run on the same schema during the
  switch, and a rollback of the code does not roll back the database.
- **What do you do right after a deployment?** Automated smoke test, then watch logs and errors for a
  while.
- **Rollback or roll forward?** Code: rollback is fast and safe. Database: roll forward with a fixing
  migration.
