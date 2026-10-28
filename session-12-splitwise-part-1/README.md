# Session 12: Minor Project - Splitwise Backend (Part 1)

The start of a Splitwise-like expense-sharing backend. The code lives in its own module,
[`splitwise-backend`](../splitwise-backend), which grows in Sessions 13 and 17; this folder explains
what Session 12 adds.

| # | Topic | README |
|---|---|---|
| 1 | Start the project: overview and requirement analysis | [01-overview-and-requirements](topics/01-overview-and-requirements/README.md) |
| 2 | Set up the project environment | [02-project-setup](topics/02-project-setup/README.md) |
| 3 | Design the technical architecture | [03-technical-architecture](topics/03-technical-architecture/README.md) |
| 4 | Design the project workflow | [04-project-workflow](topics/04-project-workflow/README.md) |
| 5 | Database schema and entity design | [05-schema-and-entity-design](topics/05-schema-and-entity-design/README.md) |

## What works after this session

- All entities and the full schema: users, groups with members, expenses with shares, settlements.
- Users: register (one account per email, stored lower-case), look up.
- Groups: create (creator is the first member), add members, read, list a user's groups.
- Errors: 400 invalid body, 404 unknown ids, 409 rule violations, all as Problem Details.
- 9 tests: [`SchemaDesignTest`](../splitwise-backend/src/test/java/jbdl/splitwise/SchemaDesignTest.java), [`UserAndGroupApiTest`](../splitwise-backend/src/test/java/jbdl/splitwise/UserAndGroupApiTest.java).

## Run it

```bash
./mvnw -pl splitwise-backend verify
java -jar splitwise-backend/target/splitwise-backend-1.0.0-SNAPSHOT.jar
curl -X POST localhost:8080/api/users -H 'Content-Type: application/json' -d '{"name":"Asha","email":"asha@example.com"}'
```
