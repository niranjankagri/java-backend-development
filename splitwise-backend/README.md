# Splitwise backend

A Splitwise-like expense-sharing backend, built across the course:

| Session | Adds | README |
|---|---|---|
| 12 | Requirements, architecture, entities and schema; users and groups API | [Session 12](../session-12-splitwise-part-1/README.md) |
| 13 | Expenses with equal / exact / percent splits, balances, settle-up | (next) |
| 17 | Unit tests with Mockito, Redis caching, Spring Security, validations, deployment | (later) |

```bash
./mvnw -pl splitwise-backend verify
java -jar splitwise-backend/target/splitwise-backend-1.0.0-SNAPSHOT.jar      # http://localhost:8080
```

Code: [`src/main/java/jbdl/splitwise`](src/main/java/jbdl/splitwise) (one package per feature), tests: [`src/test/java/jbdl/splitwise`](src/test/java/jbdl/splitwise).
