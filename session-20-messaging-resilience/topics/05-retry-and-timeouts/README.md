# Topic 5: Retry and timeouts

Two smaller protections, in front of the circuit breaker of topic 4.

## Timeout: never wait forever

Without a timeout, a hanging shipping-service holds one of order-service's request threads per order
until all are used up, and order-service stops answering too (a **cascading failure**).
[`ResilienceConfig.restClient`](../../src/main/java/jbdl/session20/orders/ResilienceConfig.java):

```java
SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
requestFactory.setConnectTimeout(timeout);   // to open the connection
requestFactory.setReadTimeout(timeout);      // to wait for the answer
```

`shipping.timeout=1s` in
[`application-orders.properties`](../../src/main/resources/application-orders.properties). A timeout
shows up as `ResourceAccessException`, which the retry and the circuit breaker count as a failure.

## Retry: for short hiccups

A restart, a lost packet, a moment of overload: often the second try works.
[`ResilienceConfig.retry`](../../src/main/java/jbdl/session20/orders/ResilienceConfig.java):

```java
RetryConfig.custom()
        .maxAttempts(maxAttempts)          // 3 in total (the first try + 2 retries)
        .waitDuration(wait)                // 200 ms between them
        .retryOnException(ResilienceConfig::isShippingFailure)
        .build();
```

What is **not** retried:

- a **4xx** answer: asking again gives the same answer;
- the circuit breaker's refusal (`CallNotPermittedException`): the circuit is open on purpose.

## Retry outside, circuit breaker inside

[`ShippingRates.quote`](../../src/main/java/jbdl/session20/orders/ShippingRates.java):

```java
BigDecimal fee = retry.executeSupplier(
        () -> circuitBreaker.executeSupplier(
                () -> askShippingService(city)));
```

Read from the inside out: one call; the circuit breaker runs it (or refuses it) and records the
result; the retry repeats that on failure. So the breaker sees **every attempt**: 3 failed attempts
count 3 times. That is why one failed order was enough to open the circuit in topic 4.

Without Resilience4j, the retry alone would be a loop:

```java
for (int attempt = 1; attempt <= 3; attempt++) {
    try {
        return askShippingService(city);
    } catch (ResourceAccessException | HttpServerErrorException e) {
        if (attempt == 3) throw e;
        Thread.sleep(200);
    }
}
```

## Rules for retrying

- **Only safe operations.** A `GET` for a fee can be repeated. A `POST` that charges a card cannot,
  unless the other service is idempotent (an idempotency key, like the event id of topic 2).
- **Few attempts, with a pause.** Many callers retrying at once can keep an overloaded service down
  (a "retry storm"). Production setups wait longer each time (exponential backoff,
  `IntervalFunction.ofExponentialBackoff`) and add randomness (jitter).
- **Count the total time.** 3 attempts with a 1 s read timeout and 200 ms pauses take at least 3.4 s
  when every answer is late (more if connecting is slow too). The caller's own timeout must be
  longer, or the retries are wasted.

## What it looks like

shipping-service stopped (connection refused, so each attempt fails fast): the first order after the
stop took 0.43 s for its 3 attempts and 2 pauses, then got the fallback fee (topic 4 shows the full
run).

## Tested

[`ShippingRatesTest`](../../src/test/java/jbdl/session20/orders/ShippingRatesTest.java), against the
fake shipping-service:

- one failed attempt, then success: the order gets the real fee, after 2 requests;
- every attempt fails (500): 3 requests, then the fallback fee;
- slow answers (1 s, timeout 300 ms in the test): the fallback fee in well under the 3 s that waiting
  would take.

## Interview questions

- **Which errors do you retry?** Temporary ones: timeouts, connection errors, 503. Not 4xx, not
  validation errors.
- **Why can retries make things worse?** They multiply the load on a service that is already
  struggling; backoff, jitter, few attempts and a circuit breaker limit that.
- **Retry and circuit breaker together: which one outside?** Usually retry outside, so the breaker
  counts each attempt and its refusal stops the retries at once.
- **Is it safe to retry a POST?** Only if the receiver is idempotent, for example with an idempotency
  key that lets it recognise the repeat.
