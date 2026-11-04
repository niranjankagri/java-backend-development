# Topic 3: Design the technical architecture

## Layers

```
  HTTP client (curl, mobile app, web app)
        │  JSON over HTTP
        ▼
  Controllers      UserController, GroupController        HTTP only: URLs, status codes, @Valid, DTOs
        │
        ▼
  Services         UserService, GroupService               business rules, @Transactional
        │
        ▼
  Repositories     AppUserRepository, ExpenseGroupRepository ...   Spring Data JPA
        │
        ▼
  Database         H2 now, PostgreSQL later
```

Each layer talks only to the one below it. Why:

- **Testable:** rules in services can be unit-tested without HTTP (Session 14).
- **Replaceable:** a new client (GraphQL, a queue consumer) reuses the services.
- **Clear errors:** services throw `NotFoundException` / `BusinessRuleException`; one
  [`ApiExceptionHandler`](../../../splitwise-backend/src/main/java/jbdl/splitwise/common/ApiExceptionHandler.java)
  maps them to 404 / 409 for every controller.

## Package by feature

```
jbdl.splitwise.users      AppUser, AppUserRepository, UserService, UserController, UserDtos
jbdl.splitwise.groups     ExpenseGroup, ExpenseGroupRepository, GroupService, GroupController, GroupDtos
jbdl.splitwise.expenses   Expense, ExpenseShare, Settlement, SplitType, repositories   (services in Session 13)
jbdl.splitwise.common     NotFoundException, BusinessRuleException, ApiExceptionHandler
```

The alternative, package by layer (`controllers/`, `services/`, `repositories/`), scatters one feature
over many folders. By feature, everything about groups is in `groups`, and a feature could later
become its own microservice (Session 19).

## DTOs at the edge

Controllers take and return records such as `CreateUser` and `UserView`, never entities:

| Without DTOs | With DTOs |
|---|---|
| Clients could send an `id` and overwrite rows | The request record has no `id` |
| A new entity field appears in the JSON by accident | The response shape is chosen on purpose |
| Lazy relations are serialised outside a transaction → errors | Views are built inside the service call |

## Design choices worth noting

| Choice | Reason |
|---|---|
| `AppUser`, `ExpenseGroup` instead of `User`, `Group` | `USER` and `GROUP` are reserved SQL words |
| `BigDecimal` for money, `numeric(12,2)` columns | exact amounts, no floating-point errors |
| Expenses and settlements are append-only | the history always explains the balance (N3) |
| Emails stored lower-case | `Asha@X.com` and `asha@x.com` are the same person |

## Interview questions

- **Why a service layer?** To keep business rules out of controllers and reusable across entry points.
- **Package by feature or by layer?** By feature scales better and keeps related code together.
- **Why not return entities from controllers?** Coupling, accidental data exposure, lazy-loading errors.
