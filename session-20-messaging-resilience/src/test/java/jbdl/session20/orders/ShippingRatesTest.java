package jbdl.session20.orders;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Topics 4 and 5: timeout, retry and circuit breaker around the call to shipping-service.
 *
 * The real shipping-service cannot be told to fail or to be slow, so the tests start a tiny fake
 * one with the JDK's built-in HTTP server: {@link #mode} decides how it answers, and {@link #requests}
 * counts the requests it got. The protections are built exactly as in the app ({@link ResilienceConfig}),
 * only with short waits so the tests are fast.
 */
class ShippingRatesTest {

    private static final Duration TIMEOUT = Duration.ofMillis(300);
    private static final Duration WAIT_IN_OPEN = Duration.ofMillis(200);

    /** How the fake shipping-service answers. */
    enum Mode { OK, FAIL, FAIL_ONCE, SLOW }

    private volatile Mode mode = Mode.OK;
    private final AtomicInteger requests = new AtomicInteger();
    private HttpServer fakeShippingService;
    private CircuitBreaker circuitBreaker;
    private ShippingRates rates;

    @BeforeEach
    void start() throws IOException {
        fakeShippingService = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        fakeShippingService.createContext("/rates/", exchange -> {
            int number = requests.incrementAndGet();
            boolean fail = mode == Mode.FAIL || (mode == Mode.FAIL_ONCE && number == 1);
            if (mode == Mode.SLOW) {
                sleep(Duration.ofSeconds(1));           // longer than TIMEOUT
            }
            byte[] body = (fail ? "{\"error\":\"down\"}" : "{\"city\":\"Delhi\",\"fee\":40}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(fail ? 500 : 200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        // one thread per request, so a slow answer does not hold up the next request
        fakeShippingService.setExecutor(Executors.newCachedThreadPool());
        fakeShippingService.start();

        String url = "http://localhost:" + fakeShippingService.getAddress().getPort();
        circuitBreaker = ResilienceConfig.circuitBreaker(WAIT_IN_OPEN);
        rates = new ShippingRates(ResilienceConfig.restClient(url, TIMEOUT), circuitBreaker,
                ResilienceConfig.retry(3, Duration.ofMillis(10)), new BigDecimal("99"));
    }

    @AfterEach
    void stop() {
        fakeShippingService.stop(0);
    }

    @Test
    @DisplayName("shipping-service answers: the fee is its fee, one request")
    void realFee() {
        ShippingQuote quote = rates.quote("Delhi");

        assertThat(quote.fee()).isEqualByComparingTo("40");
        assertThat(quote.source()).isEqualTo("shipping-service");
        assertThat(requests).hasValue(1);
    }

    @Test
    @DisplayName("Retry: one failed attempt is hidden by the second one")
    void retryHidesAHiccup() {
        mode = Mode.FAIL_ONCE;

        ShippingQuote quote = rates.quote("Delhi");

        assertThat(quote.source()).isEqualTo("shipping-service");
        assertThat(requests).hasValue(2);
    }

    @Test
    @DisplayName("All 3 attempts fail (500): the order gets the fallback fee")
    void fallbackAfterRetries() {
        mode = Mode.FAIL;

        ShippingQuote quote = rates.quote("Delhi");

        assertThat(quote.fee()).isEqualByComparingTo("99");
        assertThat(quote.source()).isEqualTo("fallback: shipping-service failed");
        assertThat(requests).hasValue(3);
    }

    @Test
    @DisplayName("Timeout: a slow answer counts as a failure after 300 ms, not after 1 s")
    void timeout() {
        mode = Mode.SLOW;

        long start = System.nanoTime();
        ShippingQuote quote = rates.quote("Delhi");
        Duration took = Duration.ofNanos(System.nanoTime() - start);

        assertThat(quote.source()).isEqualTo("fallback: shipping-service failed");
        // 3 attempts of about 300 ms each; waiting for the slow answers would take 3 s
        assertThat(took).isLessThan(Duration.ofMillis(2500));
    }

    @Test
    @DisplayName("Circuit breaker: after 4 failed calls it opens, and then shipping-service is not called at all")
    void opens() {
        mode = Mode.FAIL;

        rates.quote("Delhi");                       // 3 failed calls (the retry's attempts)
        ShippingQuote second = rates.quote("Delhi"); // the 4th call fails: 4 of 4 failed, the circuit opens

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(requests).hasValue(4);           // the retry does not retry a refused call
        assertThat(second.source()).isEqualTo("fallback: circuit open");

        ShippingQuote third = rates.quote("Delhi");

        assertThat(third.source()).isEqualTo("fallback: circuit open");
        assertThat(requests).hasValue(4);           // no new request: answered at once
    }

    @Test
    @DisplayName("After the wait, one trial call (HALF_OPEN): success closes the circuit again")
    void closesAgain() throws InterruptedException {
        mode = Mode.FAIL;
        rates.quote("Delhi");
        rates.quote("Delhi");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        mode = Mode.OK;                              // shipping-service is back
        Thread.sleep(WAIT_IN_OPEN.plusMillis(100).toMillis());
        ShippingQuote quote = rates.quote("Delhi");

        assertThat(quote.source()).isEqualTo("shipping-service");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
