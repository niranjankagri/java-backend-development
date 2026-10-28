# Topic 2: Add user and group management features

Session 12 had register, create group, add member and listing. Session 13 adds:

| Feature | Endpoint | Rules |
|---|---|---|
| Rename a group | `PATCH /api/groups/{groupId}` `{"name"}` | name not blank, ≤ 100 chars |
| Leave a group | `DELETE /api/groups/{groupId}/members/{userId}` | balance must be **0.00**; the creator can't leave |
| See the group's expenses | `GET /api/groups/{groupId}/expenses` | newest first, with every share |

## Why leaving needs a zero balance

If Meena owes 1,800 and could leave, her debt would vanish from the group's view and Asha would never
see it again. So [`MembershipService.leave`](../../../splitwise-backend/src/main/java/jbdl/splitwise/groups/MembershipService.java)
checks the balance first:

```
DELETE /api/groups/1/members/3
409 {"detail": "settle the balance of -1800.00 before leaving the group"}
```

## Avoiding a circular dependency

`ExpenseService` needs `GroupService` (to load groups). Leaving needs **both** (members and balances).
If `GroupService` also used `ExpenseService`, Spring would have to create each before the other: a
circular dependency (Spring Boot refuses it by default). A third, small service that uses both breaks
the circle:

```
GroupController ──▶ MembershipService ──▶ ExpenseService ──▶ GroupService
                                     └──────────────────────▶ GroupService
```

## Tests

[`ExpenseFlowApiTest.groupManagement`](../../../splitwise-backend/src/test/java/jbdl/splitwise/ExpenseFlowApiTest.java):
rename, refused leave with an open balance, allowed leave after settling, and the creator refused.
