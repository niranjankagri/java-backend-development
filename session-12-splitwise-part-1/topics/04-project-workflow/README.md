# Topic 4: Design the project workflow

## The user journey

```
register users ──▶ create a group ──▶ add members ──▶ add expenses ──▶ see balances ──▶ settle up
   (Session 12)       (Session 12)      (Session 12)     (Session 13)     (Session 13)     (Session 13)
```

## The API, step by step

| Step | Request | Response |
|---|---|---|
| Register | `POST /api/users` `{"name","email"}` | 201 + the user |
| Look up | `GET /api/users/{id}` | 200 / 404 |
| Create group | `POST /api/groups` `{"name","createdByUserId"}` | 201 + group with the creator as first member |
| Add member | `POST /api/groups/{groupId}/members` `{"userId"}` | 200 + group; 409 if already a member; 404 unknown group/user |
| Read group | `GET /api/groups/{groupId}` | 200 / 404 |
| My groups | `GET /api/users/{userId}/groups` | 200 + list |
| Add expense, balances, settle | `/api/groups/{groupId}/expenses`, `/balances`, `/settlements` | Session 13 |

## Real run against the application

```
$ curl -X POST localhost:18081/api/users -H 'Content-Type: application/json' -d '{"name":"Asha","email":"asha@example.com"}'
{"id":1,"name":"Asha","email":"asha@example.com"}

$ curl -X POST localhost:18081/api/groups -H 'Content-Type: application/json' -d '{"name":"Goa trip","createdByUserId":1}'
{"id":1,"name":"Goa trip","createdBy":{"id":1,...},"members":[{"id":1,"name":"Asha",...}]}

$ curl -X POST localhost:18081/api/groups/1/members -H 'Content-Type: application/json' -d '{"userId":2}'
{"id":1,"name":"Goa trip",...,"members":[{"id":1,"name":"Asha",...},{"id":2,"name":"Ravi",...}]}

$ curl -X POST localhost:18081/api/groups/1/members -H 'Content-Type: application/json' -d '{"userId":2}'
{"detail":"Ravi is already a member of Goa trip","instance":"/api/groups/1/members","status":409,"title":"Conflict"}

$ curl localhost:18081/api/users/2/groups
[{"id":1,"name":"Goa trip",...}]
```

## What happens inside one request ("add member")

```
GroupController.addMember          @Valid checks the body → 400 if userId is missing
  └─ GroupService.addMember        @Transactional begins
       ├─ get(groupId)              404 if unknown; loads the members
       ├─ users.get(userId)         404 if unknown
       ├─ group.addMember(user)     false → BusinessRuleException → 409
       └─ commit                    Hibernate inserts the group_member row (dirty checking, no save())
  └─ GroupView.of(group)           entity → DTO → JSON
```

Two lessons the tests taught while building it:

1. `@Email` validation runs **before** the service, so an email with spaces around it gets a 400; the
   service's own trimming only helps other callers.
2. A `Set` loaded from the database has no order: members came back in a random order until
   `@OrderBy("id")` was added on `ExpenseGroup.members`.

## Tests

[`UserAndGroupApiTest`](../../../splitwise-backend/src/test/java/jbdl/splitwise/UserAndGroupApiTest.java)
runs this workflow through MockMvc: registration, duplicate email (409), invalid body (400), the
group workflow, duplicate member (409) and unknown ids (404).
