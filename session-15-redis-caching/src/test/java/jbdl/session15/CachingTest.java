package jbdl.session15;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import jbdl.session15.rates.RateProvider;
import jbdl.session15.rates.RateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Server-side caching with the default in-memory cache (no Redis needed). The provider counts its
 * calls, so each test can see exactly when the cache answered instead of the slow provider.
 */
@SpringBootTest
class CachingTest {

    @Autowired
    private RateService rates;

    @Autowired
    private RateProvider provider;

    /** Empty cache and zero calls before each test (the beans are shared by all tests). */
    @BeforeEach
    void reset() {
        rates.clearCache();
        provider.resetCalls();
    }

    @Test
    @DisplayName("First call: cache miss (slow). Second call: cache hit (fast, provider not called)")
    void hitAndMiss() {
        long start = System.nanoTime();
        BigDecimal first = rates.rateToInr("USD");
        long firstMillis = (System.nanoTime() - start) / 1_000_000;

        start = System.nanoTime();
        BigDecimal second = rates.rateToInr("USD");
        long secondMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(first).isEqualByComparingTo(second);
        assertThat(provider.calls()).isEqualTo(1);           // only the first call reached the provider
        assertThat(firstMillis).isGreaterThanOrEqualTo(200); // the provider's delay
        assertThat(secondMillis).isLessThan(100);            // served from memory
    }

    @Test
    @DisplayName("Each key is cached separately")
    void separateKeys() {
        rates.rateToInr("USD");
        rates.rateToInr("EUR");
        rates.rateToInr("USD");

        assertThat(provider.calls()).isEqualTo(2);
    }

    @Test
    @DisplayName("@CachePut: an update replaces the cached value, so readers see it at once")
    void cachePut() {
        rates.rateToInr("GBP");

        rates.update("GBP", new BigDecimal("106.00"));

        assertThat(rates.rateToInr("GBP")).isEqualByComparingTo("106.00");
        assertThat(provider.calls()).isEqualTo(1); // the new value came from the cache, not the provider
    }

    @Test
    @DisplayName("@CacheEvict: after clearing, the next call goes to the provider again")
    void cacheEvict() {
        rates.rateToInr("USD");

        rates.clearCache();
        rates.rateToInr("USD");

        assertThat(provider.calls()).isEqualTo(2);
    }
}
