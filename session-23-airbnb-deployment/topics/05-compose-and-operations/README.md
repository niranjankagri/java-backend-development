# Topic 5: Running it with Docker Compose: health, readiness and graceful shutdown

## The stack

[`compose.yaml`](../../../airbnb-backend/compose.yaml) starts PostgreSQL 18 and the application:

```bash
cd airbnb-backend
DATABASE_PASSWORD=choose-one docker compose up --build
```

- `postgres` keeps its data in a named volume (`airbnb-db`), so it survives restarts and
  `docker compose down` (only `down -v` removes it). PostgreSQL 18 images keep their data below
  `/var/lib/postgresql`, which is where the volume is mounted.
- `app` waits until PostgreSQL is healthy (`depends_on: condition: service_healthy`, checked with
  `pg_isready`), reaches it by its service name (`postgres:5432`), and is restarted after a crash
  (`restart: unless-stopped`).

## Health, liveness and readiness

[`application.properties`](../../../airbnb-backend/src/main/resources/application.properties) exposes
only the health endpoint of Actuator, with the two probe groups; it needs no login
([`SecurityConfig`](../../../airbnb-backend/src/main/java/jbdl/airbnb/security/SecurityConfig.java)):

| URL | Question | A platform's reaction to DOWN |
|---|---|---|
| `/actuator/health/liveness` | is the process alive at all? | restart it |
| `/actuator/health/readiness` | can it take requests now (started, not shutting down)? | send no traffic to it for now |
| `/actuator/health` | everything, with its parts | for people and monitoring |

Real answers from the running container:

```
$ curl localhost:8080/actuator/health
{"components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"},"livenessState":{"status":"UP"},"ping":{"status":"UP"},
 "readinessState":{"status":"UP"},"ssl":{"status":"UP"}},"groups":["liveness","readiness"],"status":"UP"}
$ curl localhost:8080/actuator/health/readiness
{"status":"UP"}

$ docker compose ps
SERVICE    STATUS
app        Up 22 seconds (healthy)
postgres   Up 28 seconds (healthy)
```

`(healthy)` comes from the image's `HEALTHCHECK` (topic 4), which asks the readiness endpoint.

## Graceful shutdown

When a container is stopped (a new release, `docker compose restart`), it gets SIGTERM. With
`server.shutdown=graceful` the application stops accepting new requests, lets running ones finish for up
to 20 seconds (`spring.lifecycle.timeout-per-shutdown-phase`), then exits. Compose waits 30 seconds
(`stop_grace_period`) before it kills the process, so the application always gets its 20.

The log of `docker compose restart app`, followed by the new start:

```
Commencing graceful shutdown. Waiting for active requests to complete
Graceful shutdown complete
Schema "public" is up to date. No migration necessary.
Started AirbnbApplication in 14.004 seconds
```

The booking made before the restart was still there afterwards: the data lives in the volume, not in
the container.

## A real run

```
$ curl -u ravi@example.com:ravi-secret-1 -X POST localhost:8080/api/bookings -H 'Content-Type: application/json' -d '{"listingId":1,"checkIn":"2026-11-30","checkOut":"2026-12-03","guests":2}'
{"id":1,"listingId":1,"listingTitle":"Sea view flat","guestId":2,"checkIn":"2026-11-30","checkOut":"2026-12-03","nights":3,"guests":2,"totalPrice":10500.00,"status":"CONFIRMED"}
```

## Still missing for the internet (Session 24)

HTTPS (HTTP Basic sends the password with every request), a managed database with backups, a pipeline
that builds, tests and deploys every change, and a public address.

## Tested

[`ProdProfileTest`](../../../airbnb-backend/src/test/java/jbdl/airbnb/ProdProfileTest.java): the health
endpoint answers UP without a login, with the database component UP; liveness and readiness answer UP.

## Interview questions

- **Liveness vs. readiness?** Liveness: restart me if this fails. Readiness: do not route traffic to me
  right now (starting up, shutting down, a dependency missing).
- **What is graceful shutdown, and why does it matter in deployments?** Finishing running requests
  before exiting, so a rolling release does not cut users off mid-request.
- **Where does a container's data live?** Not in the container: in a volume (or a managed database), or
  it is gone with the container.
- **Why `depends_on` with `service_healthy`?** A started database container is not yet a database that
  accepts connections.
