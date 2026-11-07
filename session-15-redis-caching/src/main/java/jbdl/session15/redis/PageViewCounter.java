package jbdl.session15.redis;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis used directly, not only as a cache: StringRedisTemplate sends Redis commands. Each method
 * notes the Redis command it runs, so you can try the same in redis-cli.
 *
 * The bean exists in every profile (the Redis client connects only on first use), but its methods need
 * a running Redis server: the "redis" profile, or the Redis tests with Testcontainers.
 */
@Component
public class PageViewCounter {

    private final StringRedisTemplate redis;

    public PageViewCounter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** INCR page:views:{page} - atomic in Redis, so many app instances can count at the same time. */
    public long countView(String page) {
        Long views = redis.opsForValue().increment("page:views:" + page);
        return views == null ? 0 : views;
    }

    /** SET otp:{phone} {code} EX 300 - a value that deletes itself after 5 minutes. */
    public void storeOneTimeCode(String phone, String code) {
        redis.opsForValue().set("otp:" + phone, code, Duration.ofMinutes(5));
    }

    /** GET otp:{phone} - null when it never existed or has expired. */
    public String oneTimeCode(String phone) {
        return redis.opsForValue().get("otp:" + phone);
    }

    /** TTL otp:{phone} - seconds left before Redis deletes the key. */
    public long secondsLeft(String phone) {
        Long seconds = redis.getExpire("otp:" + phone);
        return seconds == null ? -2 : seconds;
    }
}
