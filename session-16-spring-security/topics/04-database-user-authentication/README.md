# Topic 4: Configure database-based user authentication

## The pieces

```
login attempt (asha + password)
   │
   ▼
Spring Security ──▶ DatabaseUserDetailsService.loadUserByUsername("asha")
                        └─ AccountRepository.findByUsername("asha")  →  Account row: hash, role, enabled
   │
   ▼
PasswordEncoder.matches(typedPassword, storedHash)   →  true: logged in with ROLE_USER / false: 401
```

| Class | Job |
|---|---|
| [`Account`](../../src/main/java/jbdl/session16/users/Account.java) | Entity: username (unique), **password hash**, role, enabled |
| [`AccountRepository`](../../src/main/java/jbdl/session16/users/AccountRepository.java) | Spring Data: `findByUsername`, `existsByUsername` |
| [`DatabaseUserDetailsService`](../../src/main/java/jbdl/session16/security/DatabaseUserDetailsService.java) | Turns an `Account` into Spring Security's `UserDetails` |
| `PasswordEncoder` bean (BCrypt) | Hashes on registration, checks on login |
| [`AccountService`](../../src/main/java/jbdl/session16/users/AccountService.java) | Registration (hash, then save), admin actions |

Defining a `UserDetailsService` bean is enough: Spring Boot wires it and the `PasswordEncoder` into
the login automatically.

## Never store passwords

| Stored | Problem |
|---|---|
| The password itself | A database leak exposes every password (and users reuse them elsewhere) |
| A fast hash (SHA-256, Session 4) | Billions of guesses per second on a GPU |
| **BCrypt / Argon2 / PBKDF2** | Deliberately slow + a random **salt** per password |

A BCrypt hash looks like `$2a$10$...`: version `2a`, cost `10` (2^10 rounds), then salt and hash. The
tests check that the stored value starts with `$2a$`, doesn't contain the password, matches with
`passwordEncoder.matches(...)`, and that the same password hashed twice gives **two different** hashes.

## Registration and account rules

- New accounts are always `USER`; only an existing admin could promote someone.
- Usernames are unique (409 on a duplicate).
- Passwords need at least 12 characters (`@Size(min = 12)`), length beating complexity rules.
- An admin can **disable** an account; `UserDetails.disabled(true)` makes its next login fail with 401
  (test `disabledAccount`).
- Responses never contain the `Account` entity (it holds the hash): the API returns small records.

## Demo accounts

[`StartupAccounts`](../../src/main/java/jbdl/session16/users/StartupAccounts.java) creates
`admin / admin-pass-123` and `asha / asha-pass-123` at startup, hashed like any other. Fine for a
course; a real system never ships default passwords.

## Interview questions

- **How does Spring Security check a password from a database?** `UserDetailsService` loads the user; the `PasswordEncoder` compares the typed password with the stored hash.
- **Why BCrypt and not SHA-256?** BCrypt is slow and salted on purpose; SHA-256 is fast, so brute force is cheap.
- **What is a salt?** Random data mixed into each hash so identical passwords get different hashes and precomputed tables don't work.
- **How would you lock an account after failed logins?** Count failures per user (a column or Redis, Session 15) and mark it locked; `UserDetails.accountLocked(...)`.
