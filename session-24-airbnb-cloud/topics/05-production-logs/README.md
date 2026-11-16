# Topic 5: Logs in production

On a laptop, log lines are read by a person in a terminal. In production they are collected by a log
service, kept for days, and searched: "all errors of the booking code in the last hour". That works far
better when every line is data instead of text.

## Structured logs: one JSON object per line

Spring Boot writes structured logs itself, since version 3.4. One line in
[`application-prod.properties`](../../../airbnb-backend/src/main/resources/application-prod.properties):

```properties
logging.structured.format.console=ecs
```

`ecs` is the Elastic Common Schema, a widely used set of field names (`log.level`, `log.logger`,
`message`, `error.stack_trace` ...); `logstash` and `gelf` are the other built-in formats. Without the
prod profile, logs stay plain text for people.

Two real lines from the container:

```json
{"@timestamp":"2026-10-31T08:44:59.085020091Z","log":{"level":"INFO","logger":"org.flywaydb.core.internal.command.DbMigrate"},"process":{"pid":1,"thread":{"name":"main"}},"service":{"name":"airbnb","node":{}},"message":"Successfully applied 2 migrations to schema \"public\", now at version v2 (execution time 00:00.406s)","ecs":{"version":"8.11"}}
{"@timestamp":"2026-10-31T08:45:07.030582943Z","log":{"level":"INFO","logger":"jbdl.airbnb.AirbnbApplication"},"process":{"pid":1,"thread":{"name":"main"}},"service":{"name":"airbnb","node":{}},"message":"Started AirbnbApplication in 16.069 seconds (process running for 17.097)","tags":["COMMONS-LOGGING"],"ecs":{"version":"8.11"}}
```

`service.name` comes from `spring.application.name`. A log service can now filter by
`log.level = ERROR` and `log.logger = jbdl.airbnb.bookings.*` without guessing the text format, and a
multi-line stack trace stays inside one record instead of becoming 80 separate lines.

## Where the logs go

The application writes to standard output only, never to files inside the container: the container
platform collects stdout (`docker compose logs app`, the Logs tab on Render) and can forward it to a log
service. A container's files are gone with the container.

## What never goes into a log

Passwords, tokens, the `Authorization` header, full card or ID numbers, and other personal data beyond
what is needed. Spring Security does not log credentials; our own code logs ids and states, not request
bodies. Logs are read by more people, and kept longer, than the database.

## Health and logs together

| Signal | Answers | Where |
|---|---|---|
| readiness / liveness | can it take traffic? is it alive? | `/actuator/health/*` (Session 23), used by the platform |
| logs | what happened, and when? | stdout, as JSON |
| metrics (not built here) | how much, how fast? (requests per second, latency, pool usage) | `/actuator/metrics` or Prometheus, with Micrometer |

## Interview questions

- **Why structured logs?** Machines can search, filter and aggregate fields; a stack trace stays one
  record; no fragile text parsing.
- **Why log to stdout in a container?** The platform owns collection and retention; files in a
  container are lost with it.
- **What must not be logged?** Secrets and unnecessary personal data.
- **Logs vs. metrics vs. health checks?** Events in detail, numbers over time, and a yes/no for the
  platform.
