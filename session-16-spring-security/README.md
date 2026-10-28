# Session 16: Spring Security & Authentication

A personal notes API protected by Spring Security: accounts with BCrypt-hashed passwords in the
database, HTTP Basic login, URL rules and method rules.

| # | Topic | Code | README |
|---|---|---|---|
| 1 | Spring Security concepts and terminology | [`security/SecurityConfig`](src/main/java/jbdl/session16/security/SecurityConfig.java) | [01-spring-security-concepts](topics/01-spring-security-concepts/README.md) |
| 2 | Authentication vs authorization | [`AuthenticationTest`](src/test/java/jbdl/session16/AuthenticationTest.java) | [02-authentication-vs-authorization](topics/02-authentication-vs-authorization/README.md) |
| 3 | Implement authorization | [`notes/NoteService`](src/main/java/jbdl/session16/notes/NoteService.java), [`AuthorizationTest`](src/test/java/jbdl/session16/AuthorizationTest.java) | [03-implement-authorization](topics/03-implement-authorization/README.md) |
| 4 | Database-based user authentication | [`security/DatabaseUserDetailsService`](src/main/java/jbdl/session16/security/DatabaseUserDetailsService.java), [`users`](src/main/java/jbdl/session16/users) | [04-database-user-authentication](topics/04-database-user-authentication/README.md) |

## What you can do after this session

- Explain the filter chain, principal, authorities, roles and the security context.
- Tell authentication (401) from authorization (403) and configure HTTP Basic for a stateless API.
- Write URL rules and `@PreAuthorize` rules, including "own data or admin".
- Store users in a database with BCrypt hashes and plug them in with a `UserDetailsService`.

## Run it

```bash
./mvnw -pl session-16-spring-security verify          # 11 tests
java -jar session-16-spring-security/target/session-16-spring-security-1.0.0-SNAPSHOT.jar
curl -u asha:asha-pass-123 localhost:8080/api/me
curl -u admin:admin-pass-123 localhost:8080/api/admin/users
```
