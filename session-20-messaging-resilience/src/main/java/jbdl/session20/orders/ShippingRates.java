package jbdl.session20.orders;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Asks shipping-service for the fee, protected by a timeout, a retry and a circuit breaker
 * ({@link ResilienceConfig}). When shipping-service cannot answer, the order still goes through with
 * the fallback fee from the settings: a slightly wrong fee is better than no order at all.
 */
@Component
public class ShippingRates implements ShippingFees {

    private final RestClient shippingService;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final BigDecimal fallbackFee;

    public ShippingRates(RestClient shippingService, CircuitBreaker circuitBreaker, Retry retry,
                         @Value("${orders.fallback-shipping-fee}") BigDecimal fallbackFee) {
        this.shippingService = shippingService;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.fallbackFee = fallbackFee;
    }

    @Override
    public ShippingQuote quote(String city) {
        try {
            // Read from the inside out: askShippingService(city) is ONE call; the circuit breaker runs it
            // (or refuses it while open) and records the result; the retry runs that again on failure.
            //
            // The same without Resilience4j would look roughly like this (no circuit breaker):
            //   for (int attempt = 1; attempt <= 3; attempt++) {
            //       try { return askShippingService(city); }
            //       catch (ResourceAccessException | HttpServerErrorException e) {
            //           if (attempt == 3) throw e;
            //           Thread.sleep(200);
            //       }
            //   }
            BigDecimal fee = retry.executeSupplier(
                    () -> circuitBreaker.executeSupplier(
                            () -> askShippingService(city)));
            return new ShippingQuote(fee, "shipping-service");
        } catch (CallNotPermittedException e) {
            // the circuit is open: shipping-service was not even called
            return fallback("circuit open");
        } catch (RuntimeException e) {
            if (ResilienceConfig.isShippingFailure(e)) {
                // every attempt failed (no answer, too slow, or 5xx)
                return fallback("shipping-service failed");
            }
            throw e; // anything else is a bug on our side: let it show
        }
    }

    private BigDecimal askShippingService(String city) {
        ShippingRate rate = shippingService.get().uri("/rates/{city}", city).retrieve().body(ShippingRate.class);
        if (rate == null) {
            throw new IllegalStateException("shipping-service sent an empty answer");
        }
        return rate.fee();
    }

    private ShippingQuote fallback(String reason) {
        return new ShippingQuote(fallbackFee, "fallback: " + reason);
    }

    /** shipping-service's answer; only the field we need (the consumer side of the contract, Session 19). */
    record ShippingRate(BigDecimal fee) {
    }
}
