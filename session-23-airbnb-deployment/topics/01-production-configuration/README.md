# Topic 1: Production configuration

On a laptop the Airbnb backend runs on H2 in memory, and Hibernate creates the tables at every start.
In production it needs a real database that keeps its data, settings that differ per environment,
and passwords that are in no file of the repository.

## One image, configured from outside

The same container image runs everywhere; only its **environment** differs (the "config" rule of the
twelve-factor app). Spring Boot reads environment variables directly, and a properties file can use
them with `${NAME:default}`
([`application-prod.properties`](../../../airbnb-backend/src/main/resources/application-prod.properties)):

```properties
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/airbnb}
spring.datasource.username=${DATABASE_USER:airbnb}
spring.datasource.password=${DATABASE_PASSWORD}          # no default: must be set
```

| Setting | Development and tests (`application.properties`) | Production (`prod` profile) |
|---|---|---|
| database | H2 in memory | PostgreSQL from `DATABASE_URL` |
| schema | Hibernate creates it (`create-drop`) | Flyway migrations; Hibernate only `validate`s (topic 2) |
| shutdown | | graceful, up to 20 s (topic 5) |
| health | `/actuator/health` with liveness and readiness | the same, with the list of components |

The image switches the profile on with `ENV SPRING_PROFILES_ACTIVE=prod`
([`Dockerfile`](../../../airbnb-backend/Dockerfile)), so nobody can forget it.

## Secrets

- The database password is only ever an environment variable: in the compose file it comes from the
  shell (`${DATABASE_PASSWORD:?set DATABASE_PASSWORD}` stops with that message if it is missing); on a
  cloud platform it is a secret setting of the service (Session 24).
- A local `.env` file next to `compose.yaml` is fine for convenience, but never committed.
- The image contains no secret at all: anyone who gets the image gets no password.

## Tested

[`ProdProfileTest`](../../../airbnb-backend/src/test/java/jbdl/airbnb/ProdProfileTest.java) starts the
application with `@ActiveProfiles("prod")` against a real PostgreSQL 18 in Docker (Testcontainers).
`@ServiceConnection` hands over the container's URL, user and password, the way `DATABASE_URL` and
friends do in production.

## Interview questions

- **How do you keep secrets out of the code?** Environment variables or a secret store (Vault, the cloud
  platform's secrets), read at start-up; nothing sensitive in the repository or the image.
- **Why one image for all environments?** What was tested is exactly what runs in production; only
  configuration changes, not the build.
- **Profiles or environment variables?** Profiles group settings that change together (prod uses
  PostgreSQL and Flyway); environment variables carry the values that differ per installation.
