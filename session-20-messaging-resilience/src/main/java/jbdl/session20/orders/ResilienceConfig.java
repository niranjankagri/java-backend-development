package jbdl.session20.orders;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * The three protections around the calls to shipping-service, each with its own job:
 * <ul>
 *   <li>timeout (topic 5): never wait longer than {@code shipping.timeout} for one answer</li>
 *   <li>retry (topic 5): try again a few times, for short hiccups</li>
 *   <li>circuit breaker (topic 4): stop calling for a while when most calls fail anyway</li>
 * </ul>
 * The static methods build them from plain values, so {@code ShippingRatesTest} can build the very
 * same objects with short waits, without Spring.
 */
@Configuration
public class ResilienceConfig {

    @Bean
    RestClient shippingRestClient(@Value("${shipping.url}") String url, @Value("${shipping.timeout}") Duration timeout) {
        return restClient(url, timeout);
    }

    @Bean
    CircuitBreaker shippingCircuitBreaker(@Value("${shipping.circuit-breaker.wait-in-open}") Duration waitInOpen) {
        return circuitBreaker(waitInOpen);
    }

    @Bean
    Retry shippingRetry(@Value("${shipping.retry.max-attempts}") int maxAttempts,
                        @Value("${shipping.retry.wait}") Duration wait) {
        return retry(maxAttempts, wait);
    }

    /** RestClient (Session 9) that gives up after {@code timeout}, to connect and to read the answer. */
    public static RestClient restClient(String url, Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder().baseUrl(url).requestFactory(requestFactory).build();
    }

    /**
     * The circuit breaker. It watches the last 4 calls; when at least half of them failed, it OPENS:
     * for {@code waitInOpen} every call is refused at once, without even trying shipping-service.
     * Then it lets ONE trial call through (HALF_OPEN): success closes it again, failure opens it again.
     */
    public static CircuitBreaker circuitBreaker(Duration waitInOpen) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(4)              // look at the last 4 calls
                .minimumNumberOfCalls(4)           // and judge only once there are 4
                .failureRateThreshold(50)          // open at 50 % failures or more
                .waitDurationInOpenState(waitInOpen)
                .permittedNumberOfCallsInHalfOpenState(1)
                // which exceptions count as "shipping-service failed": no answer / timeout, or 5xx
                .recordException(ResilienceConfig::isShippingFailure)
                .build();
        return CircuitBreaker.of("shipping", config);
    }

    /** Retry: up to {@code maxAttempts} attempts, {@code wait} apart, only for real failures. */
    public static Retry retry(int maxAttempts, Duration wait) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(wait)
                // Retry only what may work next time. NOT retried: a 4xx answer (asking again gives the
                // same answer), and the circuit breaker's "circuit open" refusal (it is open on purpose).
                .retryOnException(ResilienceConfig::isShippingFailure)
                .build();
        return Retry.of("shipping", config);
    }

    /**
     * ResourceAccessException: no connection, or no answer within the timeout.
     * HttpServerErrorException: shipping-service answered with a 5xx error.
     */
    static boolean isShippingFailure(Throwable e) {
        return e instanceof ResourceAccessException || e instanceof HttpServerErrorException;
    }
}
