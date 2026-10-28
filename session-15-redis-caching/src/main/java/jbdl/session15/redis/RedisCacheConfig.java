package jbdl.session15.redis;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Makes cache writes to Redis finish BEFORE the cached method returns.
 *
 * Spring Data Redis 4 can write cache entries asynchronously: @Cacheable returns the value and the
 * write to Redis completes a moment later. That's faster, but the tests caught the side effects:
 * an immediate second call sometimes missed the cache (the value wasn't stored yet), and an eviction
 * sometimes ran before the write landed, leaving the key behind. immediateWrites() trades a little
 * speed for predictable behaviour, which is what a learning project (and most apps) want.
 *
 * This customizer is only used when the cache type is redis (the "redis" profile).
 */
@Configuration
public class RedisCacheConfig {

    @Bean
    RedisCacheManagerBuilderCustomizer immediateRedisCacheWrites(RedisConnectionFactory connectionFactory) {
        return builder -> builder.cacheWriter(
                RedisCacheWriter.create(connectionFactory, writer -> writer.immediateWrites()));
    }
}
