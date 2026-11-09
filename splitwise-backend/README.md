# Splitwise backend

A Splitwise-like expense-sharing backend, built across the course:

| Session | Adds | README |
|---|---|---|
| 12 | Requirements, architecture, entities and schema; users and groups API | [Session 12](../session-12-splitwise-part-1/README.md) |
| 13 | Expenses with equal / exact / percent splits, balances, settle-up, rename/leave group | [Session 13](../session-13-splitwise-part-2/README.md) |
| 17 | Mockito tests, cached balances, login + roles, custom validation, Docker stack with PostgreSQL + Redis | [Session 17](../session-17-splitwise-advanced/README.md) |

```bash
./mvnw -pl splitwise-backend verify
java -jar splitwise-backend/target/splitwise-backend-1.0.0-SNAPSHOT.jar      # http://localhost:8080
```

Code: [`src/main/java/jbdl/splitwise`](src/main/java/jbdl/splitwise) (one package per feature), tests: [`src/test/java/jbdl/splitwise`](src/test/java/jbdl/splitwise).

From Session 17 on, every API call except registration and the health check needs a login (HTTP Basic with email and password), and groups take their creator from the login. The curl examples in the Session 12-13 READMEs show the API as it was then.
