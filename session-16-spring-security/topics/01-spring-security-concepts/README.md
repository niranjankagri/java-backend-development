# Topic 1: Spring Security concepts and terminology

Adding `spring-boot-starter-security` puts a **chain of filters** in front of every request. Each
filter does one job; together they decide whether the request reaches your controller.

```
request ──▶ SecurityFilterChain ───────────────────────────────────────────────────▶ DispatcherServlet ──▶ controller
             │ BasicAuthenticationFilter     reads "Authorization: Basic ..." and logs the user in
             │ ... (CSRF, session, headers)
             │ ExceptionTranslationFilter     turns security exceptions into 401 / 403
             │ AuthorizationFilter            checks the URL rules (hasRole, authenticated, permitAll)
```

## The vocabulary

| Term | Meaning | In this module |
|---|---|---|
| **Principal** | The logged-in user (usually their username) | `authentication.getName()` → `"asha"` |
| **Credentials** | What proves who they are | the password (sent with HTTP Basic) |
| **Authentication** | The object describing a login: principal + authorities + "authenticated?" | the `Authentication` parameter in [`ApiController`](../../src/main/java/jbdl/session16/notes/ApiController.java) |
| **GrantedAuthority** | A permission string | `ROLE_USER`, `ROLE_ADMIN` |
| **Role** | An authority with the `ROLE_` prefix; `hasRole("ADMIN")` checks `ROLE_ADMIN` | [`Role`](../../src/main/java/jbdl/session16/users/Role.java) |
| **SecurityContext** | Where the current `Authentication` lives during a request (per thread) | filled by the filter, read by `@PreAuthorize` |
| **UserDetailsService** | Loads a user by username for the login check | [`DatabaseUserDetailsService`](../../src/main/java/jbdl/session16/security/DatabaseUserDetailsService.java) |
| **PasswordEncoder** | Hashes passwords and checks a typed password against a hash | `BCryptPasswordEncoder` |
| **SecurityFilterChain** | Your rules: which URLs need what, which login method | [`SecurityConfig`](../../src/main/java/jbdl/session16/security/SecurityConfig.java) |

A real login's authorities in Spring Security 7 (from the running app):

```
GET /api/me  →  {"username":"asha","authorities":["ROLE_USER","FACTOR_PASSWORD"]}
```

`FACTOR_PASSWORD` is new in version 7: it records **how** the user authenticated (a password), so an
application can require a second factor (e.g. a one-time code) for sensitive actions.

## Defaults when you add the starter

With no configuration, Spring Boot protects **every** URL, generates a random password for a user
named `user` (printed in the log), and enables a login form, HTTP Basic and CSRF protection. Writing
your own `SecurityFilterChain` bean replaces those defaults.

## Interview questions

- **What is the SecurityFilterChain?** The ordered servlet filters that authenticate and authorize each request.
- **Principal vs authority?** Who the user is vs what they're allowed to do.
- **`hasRole("ADMIN")` vs `hasAuthority("ADMIN")`?** `hasRole` adds the `ROLE_` prefix; `hasAuthority` matches the exact string.
- **Where is the logged-in user stored during a request?** In the `SecurityContextHolder` (thread-local).
