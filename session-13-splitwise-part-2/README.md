# Session 13: Minor Project - Splitwise Backend (Continued)

Session 13 completes the core of the [`splitwise-backend`](../splitwise-backend) module started in
[Session 12](../session-12-splitwise-part-1/README.md): expenses, balances and settling up.

| # | Topic | README |
|---|---|---|
| 1 | Continue the backend: expense creation and settlement flows | [01-expense-and-settlement-flows](topics/01-expense-and-settlement-flows/README.md) |
| 2 | User and group management features | [02-user-and-group-management](topics/02-user-and-group-management/README.md) |
| 3 | Project HLSD (high-level system design) | [03-hlsd](topics/03-hlsd/README.md) |
| 4 | Data modelling concepts and optimisation | [04-data-modeling-and-optimization](topics/04-data-modeling-and-optimization/README.md) |
| 5 | Project completion and code review | [05-completion-and-code-review](topics/05-completion-and-code-review/README.md) |

## What's new in the code

| Part | Files |
|---|---|
| Split strategies (Strategy + factory) | [`expenses/split`](../splitwise-backend/src/main/java/jbdl/splitwise/expenses/split) |
| Expenses, balances, settle-up, settlements | [`ExpenseService`](../splitwise-backend/src/main/java/jbdl/splitwise/expenses/ExpenseService.java), [`ExpenseController`](../splitwise-backend/src/main/java/jbdl/splitwise/expenses/ExpenseController.java), [`DebtSimplifier`](../splitwise-backend/src/main/java/jbdl/splitwise/expenses/DebtSimplifier.java) |
| Aggregate queries, indexes, entity graph | [`ExpenseRepository`](../splitwise-backend/src/main/java/jbdl/splitwise/expenses/ExpenseRepository.java), [`SettlementRepository`](../splitwise-backend/src/main/java/jbdl/splitwise/expenses/SettlementRepository.java) |
| Rename group, leave group | [`GroupService`](../splitwise-backend/src/main/java/jbdl/splitwise/groups/GroupService.java), [`MembershipService`](../splitwise-backend/src/main/java/jbdl/splitwise/groups/MembershipService.java) |
| Tests (12 new, 21 in the module) | [`SplitAndSimplifyTest`](../splitwise-backend/src/test/java/jbdl/splitwise/SplitAndSimplifyTest.java), [`ExpenseFlowApiTest`](../splitwise-backend/src/test/java/jbdl/splitwise/ExpenseFlowApiTest.java) |

## Run it

```bash
./mvnw -pl splitwise-backend verify
java -jar splitwise-backend/target/splitwise-backend-1.0.0-SNAPSHOT.jar
```

Then follow the Goa trip in [topic 1](topics/01-expense-and-settlement-flows/README.md#the-goa-trip-real-run).
