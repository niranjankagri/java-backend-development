# Topic 4: Work with a local Redis server

## Start Redis

The easiest way is Docker (no install on Windows needed):

```bash
docker run -d --name redis -p 6379:6379 redis:7.4-alpine
docker exec -it redis redis-cli          # an interactive Redis shell
127.0.0.1:6379> SET greeting hello EX 60
127.0.0.1:6379> GET greeting
127.0.0.1:6379> TTL greeting
```

## Run the application against it

```bash
./mvnw -pl session-15-redis-caching package -DskipTests
java -jar session-15-redis-caching/target/session-15-redis-caching-1.0.0-SNAPSHOT.jar --spring.profiles.active=redis
```

[`application-redis.properties`](../../src/main/resources/application-redis.properties):

```properties
spring.cache.type=redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.cache.redis.time-to-live=10m
```

## Real run (Redis in Docker on port 16379, app on 18083)

```
call 1: 200 in 2.912684s     ← cache miss: 200 ms provider delay + first-request warm-up (Spring, Redis connection)
call 2: 200 in 0.034314s     ← cache hit, served from Redis

$ docker exec jbdl-redis redis-cli KEYS '*'
rates::USD
$ docker exec jbdl-redis redis-cli TTL 'rates::USD'
597                          ← seconds left of the 10-minute TTL
```

## Testing with a real Redis: Testcontainers

[`RedisTest`](../../src/test/java/jbdl/session15/RedisTest.java) doesn't need a Redis you started:

```java
@Testcontainers(disabledWithoutDocker = true)    // skip, don't fail, on machines without Docker
class RedisTest {
    @Container
    @ServiceConnection(name = "redis")           // tells Spring Boot the container's host and port
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
```

Testcontainers starts a fresh Redis in Docker for the test class and removes it afterwards. CI
(GitHub Actions on Linux) has Docker, so these tests run on every pull request.

### What the real Redis taught us

The in-memory tests always passed. Against a real Redis, `RedisTest` failed **sometimes** (2 of the
first runs): a second `rateToInr("USD")` right after the first still called the slow provider, and
after `clearCache()` the key `rates::EUR` was occasionally still there.

The cause: Spring Data Redis 4 can **write cache entries asynchronously**. `@Cacheable` returns the
value and the write to Redis finishes a moment later, so a very fast second read could miss it, and an
eviction could run before the write landed (and the write then re-created the key).
[`RedisCacheConfig`](../../src/main/java/jbdl/session15/redis/RedisCacheConfig.java) switches to
`immediateWrites()`; after that the module passed 5 runs out of 5.

(A first guess, that `Cache.clear()` itself was deferred, was tested and turned out wrong: with immediate
writes the plain `@CacheEvict` works every time, so that change was taken out again.) Flaky tests are
worth chasing to the real cause: here they pointed to genuine behaviour of the production cache.

## Tests in this module

| Class | Needs | Checks |
|---|---|---|
| `CachingTest` | nothing | hit/miss with timings, separate keys, `@CachePut`, `@CacheEvict` |
| `BrowserCacheAndCookieTest` | nothing | `Cache-Control`, `ETag`, 304, cookie attributes, cookie sent back |
| `RedisTest` | Docker | key `rates::USD` + TTL in Redis, eviction, `INCR`, `SET ... EX` |

## Interview questions

- **How do you run Redis for development?** Docker: `docker run -p 6379:6379 redis`.
- **How do you test against Redis?** Testcontainers with a real Redis, not mocks: behaviour like TTLs and eviction timing only shows up for real.
- **How do you switch between an in-memory cache and Redis?** Configuration (`spring.cache.type`), e.g. per profile.
