# Topic 2: Server cache vs browser cache

```
browser ──(1) browser cache: maybe no request at all──▶ server ──(2) server cache: skip the slow work──▶ bank API
           Cache-Control, ETag, 304                         @Cacheable, in memory or Redis
```

| | Browser (HTTP) cache | Server cache |
|---|---|---|
| Where | In each user's browser (and CDNs/proxies on the way) | In the application (memory) or a cache server (Redis) |
| Saves | The whole request, or the response body | The expensive work behind a response |
| Controlled by | Response **headers** | Code / annotations + cache configuration |
| Shared by | One user (or everyone behind a CDN) | All users |
| Invalidation | Hard: you can't reach into browsers; use short `max-age` + ETags | `@CacheEvict`, `@CachePut`, TTL |

## Server cache: Spring's cache abstraction

[`RateService`](../../src/main/java/jbdl/session15/rates/RateService.java):

| Annotation | Does |
|---|---|
| `@Cacheable("rates")` | Return the cached value if present; else run the method and store the result (key = the argument) |
| `@CachePut(value = "rates", key = "#currency")` | Always run the method and store the result (keeps the cache fresh after an update) |
| `@CacheEvict(value = "rates", allEntries = true)` | Remove entries (all of them here; or one: `key = "#currency"`) |
| `@EnableCaching` (on the application) | Switch the annotations on |

They work through a proxy, like `@Transactional`: a call from inside the same class bypasses the cache.

[`CachingTest`](../../src/test/java/jbdl/session15/CachingTest.java) proves it with the provider's
call counter: two calls for USD → 1 provider call; the first call ≥ 200 ms (the provider's delay), the
second < 100 ms.

## Browser cache: headers

[`RateController`](../../src/main/java/jbdl/session15/web/RateController.java) answers with:

```
Cache-Control: max-age=60      → the browser may reuse this answer for 60 s without asking
ETag: "USD-83.20"              → a fingerprint of this version of the answer
```

After 60 s the browser asks again with `If-None-Match: "USD-83.20"`. If nothing changed, the server
answers **304 Not Modified** with an empty body (`request.checkNotModified(etag)`), saving the
transfer. Real run:

```
$ curl -i localhost:18083/api/rates/usd
HTTP/1.1 200
Cache-Control: max-age=60
ETag: "USD-83.20"
{"currency":"USD","toInr":83.20}

$ curl -H 'If-None-Match: "USD-83.20"' localhost:18083/api/rates/usd   → 304
```

Other `Cache-Control` values: `no-store` (never cache: personal or secret data), `no-cache` (store but
always revalidate), `private` (only the user's browser, not shared CDNs), `public`.

## Interview questions

- **`@Cacheable` vs `@CachePut`?** Skip the method on a hit vs always run it and store the result.
- **What is an ETag?** A version identifier for a response, used for conditional requests (304).
- **`no-cache` vs `no-store`?** Revalidate before reuse vs never store.
- **What are the two hard things in computer science?** Cache invalidation and naming things: keep TTLs and evict on writes.
