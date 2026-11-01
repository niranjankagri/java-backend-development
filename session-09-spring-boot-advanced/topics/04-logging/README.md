# Topic 4: Overview of logging in Spring Boot applications

## The pieces

```
your code ──▶ SLF4J (the API you call: log.info(...)) ──▶ Logback (does the writing, Spring Boot's default)
                                                          ──▶ console, files, ...
```

Code only talks to **SLF4J**; the logging library behind it can be swapped (Logback, Log4j2) without
touching the code. `spring-boot-starter-webmvc` already brings both, nothing to add.

## Writing a log line

```java
log.info("order {} placed for {} (total {})", order.getId(), order.getCustomer(), order.total());
```

Use `{}` placeholders, **not** `"order " + id + ...`: the message is only built if that level is on.
Pass an exception as the **last** argument to get its stack trace: `log.error("payment failed for {}", id, e);`

## Levels

| Level | For | Example |
|---|---|---|
| `ERROR` | Something failed and needs attention | payment provider unreachable after all retries |
| `WARN` | Unexpected, but handled | `failed after 3 attempts` |
| `INFO` | Important business events | `order 1 placed for Asha` |
| `DEBUG` | Details for developers | the service trace (`-> OrderService.place(..)`) |
| `TRACE` | Very detailed internals | rarely used in application code |

A logger prints its level **and everything above it**. Configure per package in
[`application.properties`](../../src/main/resources/application.properties):

```properties
logging.level.root=INFO                 # everything: INFO and above
logging.level.jbdl.session09=DEBUG      # our code: DEBUG and above (shows the trace lines)
```

Others: `logging.file.name=target/order-desk.log` (write to a file, rolled by size),
`logging.pattern.console=...` (line format), or a full `logback-spring.xml` for advanced setups.

## Request ids with the MDC

On a busy server, log lines of many requests are mixed together. [`RequestIdFilter`](../../src/main/java/jbdl/session09/logging/RequestIdFilter.java)
gives every request an id and puts it in the **MDC** (Mapped Diagnostic Context: a small per-thread
map the logger reads). The pattern prints it with `%X{requestId}`:

```properties
logging.pattern.console=%d{HH:mm:ss.SSS} %-5level [%X{requestId:--}] %logger{36} - %msg%n
```

Real output for one request (`X-Request-Id: checkout-42`):

```
DEBUG [checkout-42] j.session09.aop.ServiceTraceAspect - -> OrderService.place(..) [OrderRequest(customer=Asha, ...)]
INFO  [checkout-42] j.session09.orders.PaymentGateway - charged 50.00 for order 1
INFO  [checkout-42] j.session09.aop.PerformanceAspect - PaymentGateway.charge(..) took 6522 us
INFO  [checkout-42] jbdl.session09.orders.OrderService - order 1 placed for Asha (total 50.00)
INFO  [checkout-42] j.session09.aop.PerformanceAspect - OrderService.place(..) took 20214 us
```

Search the logs for `checkout-42` and you get exactly this request. The id is also sent back in the
`X-Request-Id` response header, so a user can quote it. Three details in the filter:

1. It accepts the caller's id only if it's short and made of safe characters; anything else could
   inject fake lines into the log, so it's replaced with a random UUID.
2. It removes the id in `finally`: Tomcat reuses threads, and a leftover id would be printed for the
   next, unrelated request.
3. MDC works per thread. Work handed to another thread (Session 5) doesn't see it unless you copy it.

In microservices (Session 19) the same idea crosses services as a **trace id** (Micrometer Tracing).

## What not to log

Passwords, tokens, full card numbers, personal data. Log ids and outcomes instead.

## Tests

[`LoggingTest`](../../src/test/java/jbdl/session09/LoggingTest.java): a new id is generated, the
client's id is reused and appears in the log lines, an unsafe id is replaced, and the MDC is empty
after the request.

## Interview questions

- **SLF4J vs Logback?** The API (facade) vs an implementation.
- **Why `{}` placeholders?** No string building when the level is off; cleaner code.
- **How do you change log levels without a rebuild?** `application.properties`, environment variables (`LOGGING_LEVEL_JBDL=DEBUG`), or Actuator's `/actuator/loggers` at run time.
- **What is the MDC?** Per-thread key-values added to every log line: request id, user id.
