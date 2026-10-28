# Topic 1: Project overview and requirement analysis

We build the backend of a **Splitwise-like** app: friends share costs (a trip, a flat, a dinner),
the app remembers who paid what and tells everyone who owes whom.

## The problem in one example

Asha, Ravi and Meena go to Goa.

| Expense | Paid by | Split |
|---|---|---|
| Hotel 6,000 | Asha | equally among 3 → 2,000 each |
| Dinner 900 | Ravi | equally among 3 → 300 each |
| Taxi 500 | Meena | Asha 200, Ravi 300 (exact amounts) |

Instead of three people tracking nine "you owe me" notes, the app works out the **balance** of each
person and the **fewest payments** that settle the trip. That's Session 13.

## Requirements

Requirement analysis turns the idea into a list we can build and test. **Functional** requirements
say *what* the system does; **non-functional** ones say *how well*.

### Functional (with the session that delivers them)

| # | As a user I can... | Session |
|---|---|---|
| F1 | register with name and email (one account per email) | 12 |
| F2 | create a group; I become its first member | 12 |
| F3 | add other users to my group; see the groups I'm in | 12 |
| F4 | add an expense to a group: amount, who paid, how to split (equal, exact amounts, percentages) | 13 |
| F5 | see the balance of each member in a group (who is owed, who owes) | 13 |
| F6 | see the suggested repayments ("Ravi pays Asha 1,700") | 13 |
| F7 | record a repayment (settle up) | 13 |
| F8 | log in; only group members see a group's data; admins can manage users | 16, 17 |

### Non-functional

| # | Requirement | How |
|---|---|---|
| N1 | Money is always exact: no rounding drift, shares add up to the total | `BigDecimal` with 2 decimals; split rules tested (Session 13) |
| N2 | Invalid input gets a clear 4xx answer, never a 500 | validation + Problem Details (Session 8 style) |
| N3 | History is never changed: balances can always be explained | expenses and settlements are only added, never edited |
| N4 | Every rule is covered by automated tests | MockMvc + JPA tests from day one; JUnit/Mockito unit tests (Session 14, 17) |
| N5 | Fast reads of balances under load | caching with Redis (Sessions 15, 17) |
| N6 | Runs in a container with a real database | PostgreSQL + Docker (Session 17) |

### Out of scope (for now)

Currencies other than INR, recurring expenses, receipts/images, notifications. Writing down what you
**won't** build is part of requirement analysis: it stops the project from growing without end.

## Interview questions

- **Functional vs non-functional requirements?** What the system does vs qualities (performance, security, reliability).
- **How do you know a requirement is done?** It has acceptance criteria and a test.
- **Why define out-of-scope items?** To protect the timeline and set expectations.
