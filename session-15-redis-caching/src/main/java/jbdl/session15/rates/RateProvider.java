package jbdl.session15.rates;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Pretends to be a slow external service (a bank's API): every call waits 200 ms. It counts its calls,
 * so tests can prove that the cache stopped the repeated ones.
 */
@Component
public class RateProvider {

    private final Map<String, BigDecimal> ratesToInr = new ConcurrentHashMap<>(Map.of(
            "USD", new BigDecimal("83.20"),
            "EUR", new BigDecimal("90.10"),
            "GBP", new BigDecimal("105.40")));
    private final AtomicInteger calls = new AtomicInteger();

    public BigDecimal fetch(String currency) {
        calls.incrementAndGet();
        try {
            Thread.sleep(200); // the slow part a cache saves us from
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        BigDecimal rate = ratesToInr.get(currency);
        if (rate == null) {
            throw new IllegalArgumentException("unknown currency " + currency);
        }
        return rate;
    }

    public void change(String currency, BigDecimal newRate) {
        ratesToInr.put(currency, newRate);
    }

    public int calls() {
        return calls.get();
    }

    public void resetCalls() {
        calls.set(0);
    }
}
