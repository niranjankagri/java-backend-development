# Topic 4: Complete end-to-end project functionalities

The full journey now works with login, on the real production stack (PostgreSQL + Redis in Docker).

| Step | Request (as) | Real result on the Docker stack |
|---|---|---|
| Register | `POST /api/users` (anyone) | `{"id":2,"name":"Asha","email":"asha@example.com"}` (no password in the response) |
| Create a group | `POST /api/groups {"name":"Goa trip"}` (Asha) | Asha is the creator and first member |
| Add a member | `POST /api/groups/1/members {"userId":3}` (Asha) | Ravi joins |
| Add an expense | `POST /api/groups/1/expenses` Dinner 900, EQUAL (Asha) | shares 450 / 450 |
| Balances | `GET /api/groups/1/balances` (Ravi) | Asha +450.00, Ravi −450.00 |
| Outsider | `GET /api/groups/1/balances` (Eve) | 403 |
| Settle up | `GET /settle-up`, `POST /settlements` | Session 13 flows, now cached and secured |
| Support | `GET /api/admin/users` (admin) | every user with role, never a password hash |

What the database holds after that run (PostgreSQL):

```
admin@splitwise.local | ADMIN | $2a$10$...
asha@example.com      | USER  | $2a$10$...
ravi@example.com      | USER  | $2a$10$...
eve@example.com       | USER  | $2a$10$...
```

## Requirements status

All functional requirements of Session 12 (F1-F8) are now implemented and tested, including F8
(login, members-only access, an admin role). The tests that cover each are listed in
[topic 1](../01-junit-and-redis-caching/README.md).
