# Topic 2: PostgreSQL and Flyway migrations

Splitwise (Session 17) let Hibernate update the production schema (`ddl-auto=update`) and noted that
a real project would use migrations. This project does.

## Why not let Hibernate change the schema?

`update` only adds: it never renames a column, never moves data, and never removes what is gone from
the entities. Nobody reviews what it does, and no two environments are guaranteed to end up the same.
With **migrations**, every schema change is an SQL file in the repository: reviewed like code, applied
in order, recorded, and identical everywhere.

## Flyway

[`db/migration`](../../../airbnb-backend/src/main/resources/db/migration):

```
V1__create_schema.sql             the tables of Sessions 21 and 22
V2__no_overlapping_bookings.sql   the exclusion constraint (topic 3)
```

At start-up Flyway compares these files with its table `flyway_schema_history` and runs the missing
ones, in order, each once. Two rules:

- **A migration that ran is never edited.** Flyway stores a checksum and refuses to start if an applied
  file changed. A change is always a new file (`V3__...`).
- Changes must work with the running version of the application too (add a column first, use it in the
  next release, remove the old one later), because during a deployment old and new instances run side
  by side.

`V1` was written from the SQL that Hibernate itself generates for PostgreSQL (logged once with
`ddl-auto=create`), then tidied: one column per line, foreign keys inline, plus an index on
`booking.guest_id` for "my bookings".

## Hibernate only checks

```properties
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate
```

`validate`: at start-up Hibernate compares every entity with the tables and refuses to start on any
difference (a missing column, a wrong type). A mismatch between code and schema is found when the
application starts, not when a user's request hits it.

On H2 (development and tests) Flyway is off and Hibernate still creates the tables
([`application.properties`](../../../airbnb-backend/src/main/resources/application.properties)): `V2`
is PostgreSQL-only SQL.

## A real run

The first start of the container against an empty PostgreSQL 18 (log):

```
The following 1 profile is active: "prod"
Database: jdbc:postgresql://postgres:5432/airbnb (PostgreSQL 18.6)
Migrating schema "public" to version "1 - create schema"
Migrating schema "public" to version "2 - no overlapping bookings"
Successfully applied 2 migrations to schema "public", now at version v2
Started AirbnbApplication in 14.905 seconds
```

The history table afterwards:

```
$ docker compose exec postgres psql -U airbnb -d airbnb -c "select version, description, success from flyway_schema_history order by installed_rank"
 version |       description       | success
---------+-------------------------+---------
 1       | create schema           | t
 2       | no overlapping bookings | t
```

After a restart: `Schema "public" is up to date. No migration necessary.`

## Tested

[`ProdProfileTest`](../../../airbnb-backend/src/test/java/jbdl/airbnb/ProdProfileTest.java): both
migrations are recorded as successful on PostgreSQL 18, and the whole application (with Hibernate's
validation) starts on that schema; the main flow (listing, booking, the same nights again is 409,
search without the booked listing) works on PostgreSQL.

## Interview questions

- **Why migrations instead of `ddl-auto=update`?** Reviewed, ordered, repeatable changes that can also
  move data; `update` only adds and is invisible.
- **Can you change a migration that already ran?** No; write a new one. Flyway's checksum refuses an
  edited one.
- **How do you rename a column without downtime?** Expand and contract: add the new column, write both,
  move the data, switch reads, remove the old column in a later release.
- **What does `ddl-auto=validate` give you?** A failed start instead of failing requests when entities
  and schema disagree.
