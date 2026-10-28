# Topic 1: Differences between cache and cookie

Both store data "somewhere else" to avoid work, so they're easy to mix up. They answer different
questions.

| | Cache | Cookie |
|---|---|---|
| Purpose | Avoid repeating **work** (a slow call, a query) | Remember something about **one user** across requests |
| Holds | Shared data: a currency rate, a product page | Per-user data: preferred currency, a session id, a login token |
| Lives | On the server (memory, Redis) or in the browser (HTTP cache) | In the browser, sent to the server with **every** request to that site |
| Who decides | The application (what to cache, how long) | The server sets it (`Set-Cookie`), the browser stores and returns it |
| Size | MBs to GBs | ~4 KB per cookie |
| If it's lost | Slower, still correct (the data is fetched again) | The user loses the preference / gets logged out |
| Security concern | Stale data | Theft (session hijacking), so: `HttpOnly`, `Secure`, `SameSite` |

## The cookie in this module

[`PreferenceController`](../../src/main/java/jbdl/session15/web/PreferenceController.java):

```java
ResponseCookie cookie = ResponseCookie.from("currency", "EUR")
        .httpOnly(true)              // JavaScript can't read it
        .sameSite("Lax")             // not sent with most cross-site requests
        .path("/")
        .maxAge(Duration.ofDays(30)) // kept 30 days; without it, gone when the browser closes
        .build();
```

The response header the test checks: `Set-Cookie: currency=EUR; Path=/; Max-Age=2592000; ...; HttpOnly; SameSite=Lax`.
Later requests carry `Cookie: currency=EUR`, read with `@CookieValue(name = "currency", defaultValue = "INR")`.

| Cookie attribute | Meaning |
|---|---|
| `Max-Age` / `Expires` | How long the browser keeps it ("session cookie" if absent) |
| `HttpOnly` | Not readable by page JavaScript |
| `Secure` | Only sent over HTTPS (set it in production) |
| `SameSite=Lax/Strict/None` | Whether it's sent with requests from other sites (CSRF protection) |
| `Path`, `Domain` | Which URLs receive it |

## Interview questions

- **Cache vs cookie?** Shared data to save work vs per-user state carried by the browser.
- **Why `HttpOnly`?** So an XSS script can't steal the cookie (e.g. a session id).
- **Session cookie vs persistent cookie?** No expiry (deleted on close) vs `Max-Age`/`Expires`.
- **Where should a login session be stored?** Server-side session id in an `HttpOnly`, `Secure` cookie, or a token (Session 16, 18).
