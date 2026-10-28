package jbdl.session15;

import static org.assertj.core.api.Assertions.assertThat;

import jbdl.session15.rates.RateProvider;
import jbdl.session15.rates.RateService;
import jbdl.session15.redis.PageViewCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * The same caching, now in a REAL Redis server: Testcontainers starts redis:7.4-alpine in Docker for
 * this test class and stops it afterwards. @ServiceConnection hands its address to Spring Boot, so no
 * host/port configuration is needed. With no Docker available, the whole class is skipped (not failed).
 */
@SpringBootTest
@ActiveProfiles("redis")
@Testcontainers(disabledWithoutDocker = true)
class RedisTest {

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private RateService rates;

    @Autowired
    private RateProvider provider;

    @Autowired
    private PageViewCounter counter;

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    void reset() {
        rates.clearCache();
        provider.resetCalls();
    }

    @Test
    @DisplayName("@Cacheable stores the rate in Redis under the key rates::USD, with a 10-minute TTL")
    void cachedInRedis() {
        rates.rateToInr("USD");
        rates.rateToInr("USD");

        assertThat(provider.calls()).isEqualTo(1);
        assertThat(redis.hasKey("rates::USD")).isTrue();
        assertThat(redis.getExpire("rates::USD")).isBetween(1L, 600L); // spring.cache.redis.time-to-live=10m
    }

    @Test
    @DisplayName("@CacheEvict(allEntries) removes the keys from Redis")
    void evictedFromRedis() {
        rates.rateToInr("EUR");

        rates.clearCache();

        assertThat(redis.hasKey("rates::EUR")).isFalse();
    }

    @Test
    @DisplayName("INCR: an atomic counter shared by every instance of the application")
    void counter() {
        long before = counter.countView("home");

        counter.countView("home");
        counter.countView("home");

        assertThat(counter.countView("home")).isEqualTo(before + 3);
    }

    @Test
    @DisplayName("SET with EX: a one-time code that Redis deletes by itself after 5 minutes")
    void expiringValue() {
        counter.storeOneTimeCode("9800000001", "4821");

        assertThat(counter.oneTimeCode("9800000001")).isEqualTo("4821");
        assertThat(counter.secondsLeft("9800000001")).isBetween(290L, 300L);
        assertThat(counter.oneTimeCode("0000000000")).isNull();
    }
}
