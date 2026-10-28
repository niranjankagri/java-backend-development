# Topic 5: Prepare the project for deployment

## What "ready to deploy" means here

| Need | Done with |
|---|---|
| A real database | PostgreSQL in the `prod` profile ([`application-prod.properties`](../../../splitwise-backend/src/main/resources/application-prod.properties)) |
| A shared cache | Redis in the `prod` profile |
| No secrets in code or image | `DATABASE_PASSWORD`, `SPLITWISE_ADMIN_PASSWORD` from environment variables (`${...}`) |
| A health check for the platform | Spring Boot Actuator `/actuator/health` (db, redis, disk) |
| A container image | [`Dockerfile`](../../../splitwise-backend/Dockerfile): two stages, non-root user |
| One command to run everything | [`compose.yaml`](../../../splitwise-backend/compose.yaml): app + PostgreSQL + Redis with health checks |
| Proof the prod config works | [`ProdProfileSmokeTest`](../../../splitwise-backend/src/test/java/jbdl/splitwise/ProdProfileSmokeTest.java) on real PostgreSQL + Redis (Testcontainers) |

## Run the stack

```bash
cd splitwise-backend
DATABASE_PASSWORD=choose-one SPLITWISE_ADMIN_PASSWORD=choose-another docker compose up --build
curl localhost:8080/actuator/health
```

`compose.yaml` refuses to start without the two passwords (`${DATABASE_PASSWORD:?set DATABASE_PASSWORD}`),
and starts the app only after PostgreSQL and Redis report healthy. Real result:

```
UP  db: UP (PostgreSQL)  redis: UP  diskSpace: UP  livenessState: UP  readinessState: UP
```

Image build ~1 minute after the first time; image size 595 MB (JRE + Spring Boot jar).

## The Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk AS build          # stage 1: build the jar, then thrown away
RUN apt-get update && apt-get install -y --no-install-recommends unzip && ...
WORKDIR /src
COPY . .
RUN ./mvnw -B -q -pl splitwise-backend -am package -DskipTests

FROM eclipse-temurin:21-jre                    # stage 2: runtime only
RUN useradd --system --uid 10001 splitwise
COPY --from=build /src/splitwise-backend/target/splitwise-backend-1.0.0-SNAPSHOT.jar app.jar
USER splitwise                                 # never root
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

A [`.dockerignore`](../../../.dockerignore) keeps `target/` folders and `.git` out of the build context.

## Problems found on the way (and their real causes)

1. **Health was 503 in development.** With the Redis library present but no Redis running, the Redis
   health check was DOWN and pulled the whole status down. `management.health.redis.enabled=false` in
   the default profile; the prod profile switches it back on.
2. **The image build failed: "Failed to validate Maven distribution SHA-256".** Verbose output showed
   the wrapper had downloaded `apache-maven-3.10.0-bin.tar.gz`: the Linux `mvnw` script falls back to
   the tar.gz when `unzip` isn't installed (the temurin image has none), but the pinned checksum is the
   zip's. Installing `unzip` fixed it. GitHub's CI runners have `unzip`, so CI never showed it. (A first
   guess, Windows line endings in the properties file, was tested and ruled out.)

## Still to do for a real production deployment

Flyway migrations instead of `ddl-auto=update`; HTTPS in front (a load balancer or gateway, Session 22);
logs shipped to a central place and metrics (Session 23); more than one app instance (Session 24).
