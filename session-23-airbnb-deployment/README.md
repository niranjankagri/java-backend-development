# Session 23: Deployment of the Airbnb Backend Application

The Airbnb backend of Sessions 21 and 22 ([`airbnb-backend`](../airbnb-backend)) made ready to run in
production: PostgreSQL with Flyway migrations, a database constraint against double bookings, a
container image, and a Docker Compose stack with health checks and graceful shutdown.

| # | Topic | README |
|---|---|---|
| 1 | Production configuration: one image, settings and secrets from the environment | [01-production-configuration](topics/01-production-configuration/README.md) |
| 2 | PostgreSQL and Flyway migrations | [02-postgresql-and-flyway](topics/02-postgresql-and-flyway/README.md) |
| 3 | The database as the second guard against double bookings | [03-database-guard-against-double-bookings](topics/03-database-guard-against-double-bookings/README.md) |
| 4 | The container image: two stages, layers, non-root | [04-container-image](topics/04-container-image/README.md) |
| 5 | Docker Compose: health, readiness, graceful shutdown | [05-compose-and-operations](topics/05-compose-and-operations/README.md) |

## What works after this session

- `prod` profile: PostgreSQL from `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`; the schema from
  Flyway (`V1`, `V2`), checked by Hibernate (`ddl-auto=validate`).
- PostgreSQL refuses overlapping confirmed bookings by itself (exclusion constraint); such a refusal is
  answered 409, never 500.
- A layered, non-root image with a health check; `docker compose up` starts it with PostgreSQL 18.
- `/actuator/health` with liveness and readiness, open without a login; graceful shutdown.
- 50 tests in the module; the 5 new ones in
  [`ProdProfileTest`](../airbnb-backend/src/test/java/jbdl/airbnb/ProdProfileTest.java) run the prod
  profile on a real PostgreSQL 18 (Testcontainers, skipped without Docker).

## What you can do after this session

- Keep one image for every environment and configure it from outside; keep secrets out of code and images.
- Own a schema with migrations, and let Hibernate only validate it.
- Add a database constraint as a second line of defence, and explain why the code check stays.
- Write a multi-stage Dockerfile with layers, a non-root user and a health check.
- Run an application and its database with Compose, with health-based start order, probes and graceful
  shutdown.

## Run it

```bash
./mvnw -pl airbnb-backend verify                     # 50 tests; ProdProfileTest needs Docker
cd airbnb-backend
DATABASE_PASSWORD=choose-one docker compose up --build
curl localhost:8080/actuator/health
```

Then the calls of [Session 22](../session-22-airbnb-part-2/README.md#run-it) work against PostgreSQL.
`docker compose down` stops everything and keeps the data; `docker compose down -v` deletes it.
