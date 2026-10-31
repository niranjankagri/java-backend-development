# Topic 4: Circuit breaker

When shipping-service is down, every order still tries to call it, waits for the timeout, retries,
and fails. That wastes time on every request, and keeps hammering a service that is trying to come
back. A **circuit breaker** notices that most calls fail and stops calling for a while.

## Three states

```
            most of the last calls failed
   CLOSED ─────────────────────────────────▶ OPEN
     ▲                                         │  every call refused at once
     │ the trial call works                    │  (no request is sent)
     │                                         │ after waitInOpen
   HALF_OPEN ◀─────────────────────────────────┘
     │ the trial call fails
     └──────────────────────────────────────▶ OPEN again
```

- **CLOSED**: normal. Calls go through; the breaker counts successes and failures.
- **OPEN**: calls are refused immediately with `CallNotPermittedException`; the caller uses its
  fallback.
- **HALF_OPEN**: after the wait, one trial call goes through. Success closes the circuit, failure
  opens it again.

## The settings

[`ResilienceConfig.circuitBreaker`](../../src/main/java/jbdl/session20/orders/ResilienceConfig.java),
with Resilience4j:

```java
CircuitBreakerConfig.custom()
        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
        .slidingWindowSize(4)              // look at the last 4 calls
        .minimumNumberOfCalls(4)           // and judge only once there are 4
        .failureRateThreshold(50)          // open at 50 % failures or more
        .waitDurationInOpenState(waitInOpen)          // 10 s in the app
        .permittedNumberOfCallsInHalfOpenState(1)
        .recordException(ResilienceConfig::isShippingFailure)   // timeouts, no connection, 5xx
        .build();
```

Small numbers so the behaviour is easy to see; real services look at more calls (say the last 50, or
the last 60 seconds with `TIME_BASED`). Only real failures count: a 4xx answer means shipping-service
works and said no.

## Fallback

[`ShippingRates.quote`](../../src/main/java/jbdl/session20/orders/ShippingRates.java) decides what an
order gets when shipping-service cannot answer:

```java
} catch (CallNotPermittedException e) {
    return fallback("circuit open");              // shipping-service was not even called
} catch (RuntimeException e) {
    if (ResilienceConfig.isShippingFailure(e)) {
        return fallback("shipping-service failed");   // every attempt failed
    }
    throw e;
}
```

The fallback fee (99, from config-server) keeps orders working. The answer says where the fee came from
(`shippingFeeSource`), so a fallback is never hidden. Whether a fallback is acceptable is a business
decision: for a price or a payment there is often no safe default, and the right answer is a clear
503.

## What it looks like

shipping-service stopped, three orders in a row, the breaker's state after each (`GET
/shipping/circuit-breaker`, [`OrderController`](../../src/main/java/jbdl/session20/orders/OrderController.java)).
Real run; one earlier call had succeeded:

```
{"id":3,...,"shippingFee":99,"shippingFeeSource":"fallback: shipping-service failed","total":399}   (0.43 s)
{"state":"OPEN","failureRate":75.0,"successfulCalls":1,"failedCalls":3,"refusedCalls":0}
{"id":4,...,"shippingFee":99,"shippingFeeSource":"fallback: circuit open","total":399}              (0.013 s)
{"state":"OPEN","failureRate":75.0,"successfulCalls":1,"failedCalls":3,"refusedCalls":1}
{"id":5,...,"shippingFee":99,"shippingFeeSource":"fallback: circuit open","total":399}              (0.015 s)
{"state":"OPEN","failureRate":75.0,"successfulCalls":1,"failedCalls":3,"refusedCalls":2}
```

The first order made 3 attempts (topic 5): 1 success + 3 failures = 75 % of 4 calls, so the circuit
opened. The next orders were answered in about 13 ms instead of 430 ms, without any request to
shipping-service.

shipping-service started again, 10 s later:

```
{"id":6,...,"shippingFee":40,"shippingFeeSource":"shipping-service","total":340}
{"state":"CLOSED","failureRate":-1.0,"successfulCalls":0,"failedCalls":0,"refusedCalls":0}
```

The trial call worked, the circuit closed, and the counting starts again (`-1`: not enough calls yet).

## Tested

[`ShippingRatesTest`](../../src/test/java/jbdl/session20/orders/ShippingRatesTest.java) uses a small
fake shipping-service (the JDK's built-in HTTP server) that can answer, fail or be slow, and counts its
requests:

- after 4 failed calls the circuit is OPEN, and the next quote sends **no request** at all;
- after the wait (200 ms in the test), the trial call succeeds and the circuit is CLOSED again.

[`MessagingEndToEndTest`](../../src/test/java/jbdl/session20/MessagingEndToEndTest.java) stops the real
shipping-service: the order still goes through with config-server's fallback fee.

## Annotations instead of code

Many projects write the same with an annotation (resilience4j-spring-boot, AOP):

```java
@CircuitBreaker(name = "shipping", fallbackMethod = "fallbackFee")
public ShippingQuote quote(String city) { ... }
```

with the settings in `application.properties` (`resilience4j.circuitbreaker.instances.shipping.*`).
Shorter, but the order of retry and circuit breaker and the fallback rules are hidden in
configuration; this session writes them out so every step is visible.

## Interview questions

- **Why a circuit breaker if there are timeouts?** A timeout still costs its full wait on every call;
  an open circuit fails in microseconds and gives the other service time to recover.
- **What happens in HALF_OPEN?** A few trial calls decide: success closes the circuit, failure opens
  it again for another wait.
- **Which errors should count?** Failures of the other service (timeouts, connection errors, 5xx), not
  4xx answers, which mean the request was wrong.
- **What is a fallback, and when is there none?** A substitute result (default value, cached value,
  reduced feature). For money or stock there is often no safe one; then fail fast with 503.
