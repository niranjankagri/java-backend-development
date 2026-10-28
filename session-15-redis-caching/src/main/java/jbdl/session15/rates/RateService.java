package jbdl.session15.rates;

import java.math.BigDecimal;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Server-side caching with Spring's cache annotations. Like @Transactional (Session 11), they work
 * through a proxy: the first call runs the method and stores the result; later calls with the same key
 * return the stored value WITHOUT running the method.
 *
 * Without the annotations, the same logic by hand would be:
 *   BigDecimal cached = cache.get(currency);
 *   if (cached != null) return cached;
 *   BigDecimal rate = provider.fetch(currency);
 *   cache.put(currency, rate);
 *   return rate;
 */
@Service
public class RateService {

    /** The cache's name; with Redis the keys become "rates::USD", "rates::EUR"... */
    public static final String CACHE = "rates";

    private final RateProvider provider;

    public RateService(RateProvider provider) {
        this.provider = provider;
    }

    /** Cache hit: return the stored rate. Cache miss: call the slow provider and store the result. */
    @Cacheable(CACHE)
    public BigDecimal rateToInr(String currency) {
        return provider.fetch(currency);
    }

    /** The rate changed: update the source AND put the new value in the cache (always runs). */
    @CachePut(value = CACHE, key = "#currency")
    public BigDecimal update(String currency, BigDecimal newRate) {
        provider.change(currency, newRate);
        return newRate;
    }

    /** Throw away every cached rate, e.g. once a day when the bank publishes new ones. */
    @CacheEvict(value = CACHE, allEntries = true)
    public void clearCache() {
        // nothing to do here: the annotation does the work
    }
}
