# Topic 2: Add Spring Security and role-based access

Session 16's approach, applied to Splitwise.

## Who may do what

| Caller | Can |
|---|---|
| Anyone | register (`POST /api/users`), `GET /actuator/health` |
| Logged-in user | create groups (they become the creator), list **their own** groups |
| Member of a group | everything about that group: members, expenses, balances, settle-up, settlements |
| `ADMIN` | `GET /api/admin/users`, and any group (support staff) |

Login is HTTP Basic with the **email** and password ([`EmailUserDetailsService`](../../../splitwise-backend/src/main/java/jbdl/splitwise/security/EmailUserDetailsService.java));
passwords are BCrypt hashes in `app_user.password_hash`.

## Membership as a rule

```java
@PreAuthorize("@groupAccess.canAccess(#groupId, authentication)")
@GetMapping("/balances")
public List<BalanceView> balances(@PathVariable long groupId) { ... }
```

`@groupAccess` is a normal bean ([`GroupAccess`](../../../splitwise-backend/src/main/java/jbdl/splitwise/security/GroupAccess.java)):
admin → yes; otherwise `groups.existsByIdAndMembersEmail(groupId, authentication.getName())`. The rule
stays one line; the logic is ordinary Java.

## API changes because of security

| Before (Session 12-13) | Now | Why |
|---|---|---|
| `POST /api/groups {"name", "createdByUserId"}` | `{"name"}`; the creator is the logged-in user | nobody can create groups in someone else's name |
| `POST /api/users {"name", "email"}` | `+ "password"` (12-100 chars) | accounts need a password |
| unknown group → 404 | 403 for a user, 404 for an admin | a 404 would tell outsiders which group ids exist |

## The first admin

[`StartupAdmin`](../../../splitwise-backend/src/main/java/jbdl/splitwise/security/StartupAdmin.java) creates
it once from `splitwise.admin.email` / `splitwise.admin.password`. Development values are in
`application.properties`; the prod profile requires the environment variable `SPLITWISE_ADMIN_PASSWORD`.

## Real check on the Docker stack

```
GET /api/groups/1                       (no login)     → 401
GET /api/groups/1/balances  -u ravi@...  (member)      → [{"name":"Asha","balance":450.00},{"name":"Ravi","balance":-450.00}]
GET /api/groups/1/balances  -u eve@...   (not a member) → 403
```

[`SecurityTest`](../../../splitwise-backend/src/test/java/jbdl/splitwise/SecurityTest.java) checks the
same plus the own-groups rule and the admin role.
