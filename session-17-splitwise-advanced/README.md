# Session 17: Splitwise Backend - Advanced Enhancements

The [`splitwise-backend`](../splitwise-backend) from Sessions 12-13, made production-ready: tests at
every level, cached balances, login with roles, stronger validation, and a Docker stack with PostgreSQL
and Redis.

| # | Topic | README |
|---|---|---|
| 1 | Integrate JUnit testing and Redis caching | [01-junit-and-redis-caching](topics/01-junit-and-redis-caching/README.md) |
| 2 | Add Spring Security and role-based access | [02-security-and-roles](topics/02-security-and-roles/README.md) |
| 3 | Advanced validations and optimisations | [03-validations-and-optimizations](topics/03-validations-and-optimizations/README.md) |
| 4 | Complete end-to-end project functionalities | [04-end-to-end-functionality](topics/04-end-to-end-functionality/README.md) |
| 5 | Prepare the project for deployment | [05-deployment-readiness](topics/05-deployment-readiness/README.md) |

## What's new in the code

| Part | Files |
|---|---|
| Security | [`security`](../splitwise-backend/src/main/java/jbdl/splitwise/security): `SecurityConfig`, `EmailUserDetailsService`, `GroupAccess`, `StartupAdmin`; `AppUser` password hash + `Role`; `AdminController` |
| Caching | [`BalanceCalculator`](../splitwise-backend/src/main/java/jbdl/splitwise/expenses/BalanceCalculator.java), `@CacheEvict` in `ExpenseService` and `GroupService` |
| Validation | [`ValidSplitValues`](../splitwise-backend/src/main/java/jbdl/splitwise/expenses/ValidSplitValues.java), [`SplitValuesValidator`](../splitwise-backend/src/main/java/jbdl/splitwise/expenses/SplitValuesValidator.java), `@Version` on `ExpenseGroup` |
| Deployment | [`application-prod.properties`](../splitwise-backend/src/main/resources/application-prod.properties), [`Dockerfile`](../splitwise-backend/Dockerfile), [`compose.yaml`](../splitwise-backend/compose.yaml), [`.dockerignore`](../.dockerignore) |
| Tests (33 in the module) | `ExpenseServiceTest` (Mockito), `SecurityTest`, `ProdProfileSmokeTest` (Testcontainers), updated API tests with login |

## Run it

```bash
./mvnw -pl splitwise-backend verify                     # the smoke test needs Docker (skipped without)
cd splitwise-backend && DATABASE_PASSWORD=... SPLITWISE_ADMIN_PASSWORD=... docker compose up --build
```
