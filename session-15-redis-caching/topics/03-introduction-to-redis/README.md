# Topic 3: Introduction to Redis

**Redis** is an in-memory data store: a separate server that keeps keys and values in RAM, so reads
and writes take well under a millisecond. Used as a cache, a counter, a session store, a rate limiter
or a simple message broker.

## Why not just a map inside the application?

| In-memory map (`spring.cache.type=simple`) | Redis |
|---|---|
| Each application instance has its own copy | One copy shared by all instances |
| Lost on restart | Survives restarts of the app (and can be persisted to disk) |
| Unlimited growth unless you add eviction | TTLs and max-memory eviction policies built in |
| Nothing to install | A server to run and monitor |

With two or more app instances behind a load balancer (Sessions 19, 24), an in-memory cache gives
different answers per instance and evicting on one doesn't evict on the others: that's when you need Redis.

## Data types and commands

| Type | Commands | Use |
|---|---|---|
| String | `SET key value [EX seconds]`, `GET`, `INCR`, `DEL` | cache entries, counters, one-time codes |
| Hash | `HSET user:1 name Asha`, `HGET`, `HGETALL` | an object's fields |
| List | `LPUSH`, `RPOP`, `LRANGE` | queues, recent items |
| Set | `SADD`, `SISMEMBER`, `SMEMBERS` | unique tags, online users |
| Sorted set | `ZADD board 120 asha`, `ZRANGE ... WITHSCORES` | leaderboards, rankings |
| Expiry | `EXPIRE key 300`, `TTL key` | anything temporary |

## Redis from Java: [`PageViewCounter`](../../src/main/java/jbdl/session15/redis/PageViewCounter.java)

`StringRedisTemplate` (from `spring-boot-starter-data-redis`) sends the commands:

| Java | Redis command |
|---|---|
| `redis.opsForValue().increment("page:views:home")` | `INCR page:views:home` (atomic: safe from many app instances) |
| `redis.opsForValue().set("otp:98...", "4821", Duration.ofMinutes(5))` | `SET otp:98... 4821 EX 300` |
| `redis.opsForValue().get("otp:98...")` | `GET otp:98...` |
| `redis.getExpire("otp:98...")` | `TTL otp:98...` |

Keys are named with colons, `type:id:field`, so they're easy to find and group.

## Redis as the cache behind `@Cacheable`

No code change: the `redis` profile sets `spring.cache.type=redis` and Spring stores every cached rate
under `rates::USD`, `rates::EUR`... with `spring.cache.redis.time-to-live=10m`.

## Interview questions

- **Why is Redis fast?** Data in RAM, a simple single-threaded command loop, efficient data structures.
- **What happens when Redis is full?** It evicts keys per `maxmemory-policy` (e.g. `allkeys-lru`) or refuses writes.
- **Is Redis a database?** It can persist (RDB snapshots, AOF log), but it's mostly used as a cache or for fast, losable data.
- **Why is `INCR` safe with many clients?** Redis runs commands one at a time: each is atomic.
