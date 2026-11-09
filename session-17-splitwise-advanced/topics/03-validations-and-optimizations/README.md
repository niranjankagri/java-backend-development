# Topic 3: Implement advanced validations and optimisations

## A custom validation constraint

Single-field annotations (`@NotNull`, `@Positive`) can't express "the values must fit the split type",
which involves two fields. A **custom constraint** on the whole request can:

```java
@ValidSplitValues
public record AddExpense(String description, BigDecimal amount, Long paidByUserId,
                         SplitType splitType, List<Participant> participants) { }
```

| Piece | File |
|---|---|
| The annotation (`@Constraint(validatedBy = ...)`, `message`, `groups`, `payload`) | [`ValidSplitValues`](../../../splitwise-backend/src/main/java/jbdl/splitwise/expenses/ValidSplitValues.java) |
| The check (`ConstraintValidator<ValidSplitValues, AddExpense>`) | [`SplitValuesValidator`](../../../splitwise-backend/src/main/java/jbdl/splitwise/expenses/SplitValuesValidator.java) |

Rules: EQUAL takes **no** values; EXACT and PERCENT need a **positive** value for **every** participant.
The validator reports the problem on the `participants` field with a specific message, so the client
gets a 400 like:

```json
{"errors": {"participants": "an EQUAL split takes no values"}}
```

Validation layers now: field annotations and the custom constraint (400, before any service code) →
business rules in the service (409: membership, totals, overpaying) → database constraints (last line).

## Optimistic locking

```java
@Version
private long version;     // on ExpenseGroup
```

Hibernate adds `WHERE id = ? AND version = ?` to every update and increments the version. If two people
rename the same group at the same moment, the second update finds a newer version and fails with
`ObjectOptimisticLockingFailureException` instead of silently overwriting the first change. "Optimistic"
because nothing is locked while reading; conflicts are detected when writing.

## Optimisations in the project so far

| Optimisation | Session |
|---|---|
| Balances from 4 aggregate queries, not by loading every expense | 13 |
| Indexes on `expense.group_id`, `expense_share.user_id`, `settlement.group_id` | 13 |
| Entity graph: the expense list loads payer and shares in one query (no N+1) | 13 |
| Cached balances per group, evicted on change | 17 |
| `open-in-view=false`: no hidden lazy loading during JSON rendering | 12 |
