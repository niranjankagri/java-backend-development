# Topic 3: Implement authorization

Two places to put rules, used together.

## 1. URL rules in the filter chain

[`SecurityConfig`](../../src/main/java/jbdl/session16/security/SecurityConfig.java):

```java
.authorizeHttpRequests(rules -> rules
        .requestMatchers("/api/public/**").permitAll()                       // anyone
        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()  // anyone may sign up
        .requestMatchers("/api/admin/**").hasRole("ADMIN")                   // only admins
        .anyRequest().authenticated())                                       // everything else: any logged-in user
```

Rules are checked **top down; the first match wins**. Put specific rules first and end with
`anyRequest()`. If `anyRequest().authenticated()` came first, the `/api/public/**` rule would never be reached.

## 2. Method rules with `@PreAuthorize`

`@EnableMethodSecurity` switches them on. They protect the **service**, so the rule holds however the
method is reached (a controller, a scheduled job, a message listener):

```java
@PreAuthorize("hasRole('ADMIN')")
public List<String> allUsernames() { ... }                     // AccountService

@PreAuthorize("#username == authentication.name or hasRole('ADMIN')")
public List<Note> notesOf(String username) { ... }             // NoteService: own notes, or an admin
```

`#username` is the method's parameter, `authentication` the current login. If the expression is false,
Spring throws `AuthorizationDeniedException` before the method runs → **403**.

| Request | Rule | Result |
|---|---|---|
| asha → `GET /api/notes/of/asha` | her own | 200 |
| asha → `GET /api/notes/of/admin` | someone else's, not an admin | 403 |
| admin → `GET /api/notes/of/asha` | admin | 200 |

The test `methodSecurityWithoutHttp` calls `AccountService.allUsernames()` **directly** with
`@WithMockUser(roles = "USER")` (a fake login for tests): it's still refused, showing the rule lives
in the service, not only on the URL.

## 3. Ownership from the login, never from the request

```java
Note note = notes.write(authentication.getName(), request.text());   // owner = the logged-in user
```

If the note's owner came from the request body, any user could write notes "as" someone else. Take
identity from the `Authentication`, always.

## About CSRF

Cross-Site Request Forgery tricks a **browser** into sending a request with the user's **cookies**.
This API uses no cookies (credentials are sent explicitly), so CSRF protection is disabled with a
comment. For an app with a login form and session cookies, keep it **on**.

## Interview questions

- **URL rules or method security?** Both: URL rules for broad areas, method rules for business-level checks (ownership).
- **Why does rule order matter?** The first matching `requestMatchers` wins.
- **What does `@PreAuthorize` need to work?** `@EnableMethodSecurity` and a call through the Spring proxy (self-invocation bypasses it, Session 9).
- **When can you disable CSRF?** For stateless APIs that don't authenticate with cookies.
