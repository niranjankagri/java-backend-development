# Session 15: Redis & Caching

Currency rates from a slow provider, made fast with Spring's cache (in memory by default, Redis with
the `redis` profile), plus browser caching headers and a cookie.

| # | Topic | Code | README |
|---|---|---|---|
| 1 | Differences between cache and cookie | [`web/PreferenceController`](src/main/java/jbdl/session15/web/PreferenceController.java) | [01-cache-vs-cookie](topics/01-cache-vs-cookie/README.md) |
| 2 | Server cache vs browser cache | [`rates/RateService`](src/main/java/jbdl/session15/rates/RateService.java), [`web/RateController`](src/main/java/jbdl/session15/web/RateController.java) | [02-server-vs-browser-cache](topics/02-server-vs-browser-cache/README.md) |
| 3 | Introduction to Redis | [`redis/PageViewCounter`](src/main/java/jbdl/session15/redis/PageViewCounter.java) | [03-introduction-to-redis](topics/03-introduction-to-redis/README.md) |
| 4 | Work with a local Redis server | [`application-redis.properties`](src/main/resources/application-redis.properties), [`RedisTest`](src/test/java/jbdl/session15/RedisTest.java) | [04-local-redis-server](topics/04-local-redis-server/README.md) |

## What you can do after this session

- Tell a cache from a cookie, and set a cookie safely (`HttpOnly`, `SameSite`, `Max-Age`).
- Cache with `@Cacheable`, `@CachePut` and `@CacheEvict`, and use `Cache-Control`, `ETag` and 304 for browsers.
- Explain when an in-memory cache isn't enough and Redis is needed; use Redis strings, counters and TTLs.
- Run Redis locally with Docker and test against a real Redis with Testcontainers.

## Run it

```bash
./mvnw -pl session-15-redis-caching verify          # 12 tests; the 4 Redis tests need Docker (skipped without)
java -jar session-15-redis-caching/target/session-15-redis-caching-1.0.0-SNAPSHOT.jar                                 # in-memory cache
java -jar session-15-redis-caching/target/session-15-redis-caching-1.0.0-SNAPSHOT.jar --spring.profiles.active=redis  # Redis on localhost:6379
curl -i localhost:8080/api/rates/usd
```
