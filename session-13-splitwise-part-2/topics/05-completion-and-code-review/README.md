# Topic 5: Project completion and code review

## Definition of done for the minor project

| Requirement (Session 12) | Status | Proof |
|---|---|---|
| F1 register users | done | `UserAndGroupApiTest` |
| F2 create a group | done | `UserAndGroupApiTest.groupWorkflow` |
| F3 add members, list groups | done | `UserAndGroupApiTest` |
| F4 expenses with EQUAL / EXACT / PERCENT | done | `SplitAndSimplifyTest`, `ExpenseFlowApiTest` |
| F5 balances | done | `ExpenseFlowApiTest.balances` |
| F6 settle-up plan | done | `ExpenseFlowApiTest.settleUpPlan` |
| F7 record a settlement | done | `ExpenseFlowApiTest.settlement` |
| F8 login, roles | Session 16-17 | |
| N1 exact money | done | paisa tests in `SplitAndSimplifyTest` |
| N2 clear 4xx errors | done | 400/404/409 tests |
| N4 tests for every rule | done | 21 tests, all green |

## Code review checklist (applied to this code)

| Check | What we looked for | Result |
|---|---|---|
| Correctness | Money adds up exactly; balances sum to 0 | Tested with remainders (100 / 3) and the Goa trip |
| Validation at the edge | Every request body has constraints | `@NotBlank`, `@Positive`, `@Digits`, `@NotEmpty` on DTOs |
| Business rules in services | Controllers only translate HTTP | Yes; controllers have no `if` |
| Transactions | Every write is one `@Transactional` service call | `addExpense`, `recordSettlement`, `leave` |
| No N+1 | Lists load related data in one query | Entity graph on the expense list |
| Readable for a beginner | No clever one-liners | `DebtSimplifier` was first written with `Object[]` arrays and casts; rewritten with a small named class `OpenAmount` during review |
| Naming | Clear, no reserved words | `AppUser`, `ExpenseGroup` |
| Errors | Specific messages | e.g. "exact amounts add up to 600.00, not 800.00" |

## Found during review / tests (fixed)

- Member order came back random from the database → `@OrderBy("id")` (Session 12).
- `@Email` rejects surrounding spaces before the service can trim them → documented, test adjusted (Session 12).
- Two Javadoc blocks on `Expense` after adding the index note → merged (the build's `-Xlint` would have failed).

## Known limits (next sessions)

- No authentication: anyone can add expenses to any group → Spring Security (16, 17).
- Balances are recalculated on every request → Redis cache (15, 17).
- Only unit-style tests with a real database; service tests with Mockito come in Session 14.

## How to review someone's pull request

1. Read the description and the requirement it claims to meet.
2. Run it (or check CI is green), then read the tests first: do they prove the requirement?
3. Read the code for correctness, then clarity, then style.
4. Comment kindly and concretely: what, why, and a suggestion.
