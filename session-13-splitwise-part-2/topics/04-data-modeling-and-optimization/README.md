# Topic 4: Data modelling concepts and optimisation

## Modelling concepts used in the project

| Concept | Meaning | In Splitwise |
|---|---|---|
| **Normalisation** | Store each fact once | A user's name is only in `app_user`; shares point to the user by id |
| **Associative (join) table** | Many-to-many between two tables | `group_member(group_id, user_id)` |
| **Weak entity** | Exists only as part of another | `expense_share` belongs to its expense (cascade + orphan removal) |
| **Append-only / ledger** | Facts are added, never edited | expenses and settlements |
| **Derived data** | Calculated, not stored | balances (from the ledger) |
| **Exact decimal type** | No floating point for money | `numeric(12,2)` + `BigDecimal` |

## Optimisation 1: let the database add up

A naive balance loads **every** expense with its shares into Java and adds them in loops: thousands of
objects for a long-running group. Instead, four aggregate queries return **one small row per member**:

```java
@Query("""
        select new jbdl.splitwise.expenses.UserTotal(e.paidBy.id, sum(e.amount))
        from Expense e
        where e.group.id = :groupId
        group by e.paidBy.id""")
List<UserTotal> totalPaidPerUser(@Param("groupId") Long groupId);
```

`select new ...UserTotal(...)` makes JPQL build a small record for each row (readable, unlike raw
`Object[]` rows). See [`ExpenseRepository`](../../../splitwise-backend/src/main/java/jbdl/splitwise/expenses/ExpenseRepository.java)
and [`SettlementRepository`](../../../splitwise-backend/src/main/java/jbdl/splitwise/expenses/SettlementRepository.java).

## Optimisation 2: indexes on the lookup columns

```java
@Table(indexes = @Index(name = "idx_expense_group", columnList = "group_id"))
public class Expense { ... }
```

Also `idx_share_user` on `expense_share(user_id)` and `idx_settlement_group` on `settlement(group_id)`.
Without an index, `WHERE group_id = ?` reads the whole table; with one, the database jumps straight to
the rows of that group (like a book's index). Indexes cost a little on every insert and some disk:
add them for columns you **search** by, not for every column.

## Optimisation 3: no N+1 when listing expenses

Listing 20 expenses would load each expense's shares and each share's user one by one (Session 11's
N+1 problem). An entity graph loads them in the same query:

```java
@EntityGraph(attributePaths = {"paidBy", "shares", "shares.user"})
List<Expense> findByGroupIdOrderByCreatedAtDesc(Long groupId);
```

## When to denormalise

If balances were read millions of times a day, we could **store** a running balance per member and
update it with every expense (faster reads, but two places to keep in sync, inside one transaction).
Caching (Session 15) gets most of that benefit with less risk, so the model stays normalised.

## Interview questions

- **What is normalisation and when would you break it?** One fact in one place; denormalise for read performance with a clear sync strategy.
- **How do indexes speed up queries, and what do they cost?** A sorted lookup structure (B-tree); slower writes and more storage.
- **Aggregate in SQL or in Java?** In SQL when the input is large: less data over the wire and fewer objects.
- **What is a ledger model?** Immutable entries; the current state is derived from them.
