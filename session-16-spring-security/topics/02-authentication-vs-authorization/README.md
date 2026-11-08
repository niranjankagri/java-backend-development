# Topic 2: Authentication vs authorization

| | Authentication | Authorization |
|---|---|---|
| Question | **Who are you?** | **What may you do?** |
| Happens | First | After authentication |
| Based on | Credentials: password, token, certificate | Roles, ownership, rules |
| Fails with | **401 Unauthorized** (misnamed: it means "unauthenticated") | **403 Forbidden** |
| In this module | HTTP Basic + database accounts + BCrypt | URL rules + `@PreAuthorize` |

Real responses from the running application:

```
GET /api/me                                     → 401   (no credentials)
GET /api/me                 -u asha:wrong       → 401   (wrong password)
GET /api/me                 -u asha:asha-pass-123 → 200 {"username":"asha",...}
GET /api/admin/users        -u asha:asha-pass-123 → 403 (logged in, but not an ADMIN)
GET /api/admin/users        -u admin:admin-pass-123 → 200 ["admin (ADMIN)","asha (USER)"]
```

A 401 also tells the client **how** to log in:

```
WWW-Authenticate: Basic realm="Realm", charset="UTF-8"
```

## HTTP Basic, explained

The client sends the username and password on **every** request:

```
Authorization: Basic YXNoYTphc2hhLXBhc3MtMTIz        ← base64("asha:asha-pass-123")
```

Base64 is an encoding, **not encryption**: anyone who sees the header can decode it. HTTP Basic is
fine for learning and machine-to-machine calls **over HTTPS only**. Web and mobile apps usually log in
once and then send a token (Session 18: OAuth 2) or use a session cookie.

## Stateless

`SessionCreationPolicy.STATELESS`: the server keeps no login session; each request proves itself.
Any instance behind a load balancer can serve any request (Session 13's HLSD).

## Tests

[`AuthenticationTest`](../../src/test/java/jbdl/session16/AuthenticationTest.java): public URL, 401
with and without the challenge header, wrong and right password, registration then login, the hash in
the database, salted hashes. [`AuthorizationTest`](../../src/test/java/jbdl/session16/AuthorizationTest.java):
the 403 cases (topic 3).

## Interview questions

- **401 vs 403?** Not authenticated vs authenticated but not allowed.
- **Is HTTP Basic secure?** Only over HTTPS; the credentials are just base64-encoded.
- **Why stateless APIs?** Scaling and simplicity: no session storage or sticky sessions.
