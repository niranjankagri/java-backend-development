# Topic 2: Implement cross-cutting concerns using Spring AOP

Four aspects, one per kind of advice. Each is a normal Spring bean with `@Aspect` + `@Component`.

## 1. Timing with `@Around`: [`PerformanceAspect`](../../src/main/java/jbdl/session09/aop/PerformanceAspect.java)

```java
@Around("@annotation(timed)")                       // every method annotated @Timed
public Object time(ProceedingJoinPoint call, Timed timed) throws Throwable {
    long start = System.nanoTime();
    try {
        return call.proceed();                      // run the real method
    } finally {
        log.info("{} took {} us", call.getSignature().toShortString(), (System.nanoTime() - start) / 1_000);
    }
}
```

`@Around` is the most powerful advice: it decides **if and when** the method runs (`proceed()`), and
can change arguments, the result, or exceptions. If you forget `proceed()`, the method never runs.

## 2. Auditing with `@AfterReturning` / `@AfterThrowing`: [`AuditAspect`](../../src/main/java/jbdl/session09/aop/AuditAspect.java)

| Advice | Runs | Can see |
|---|---|---|
| `@Before` | before the method | the arguments |
| `@AfterReturning` | after a normal return | the result (`returning = "result"`) |
| `@AfterThrowing` | after an exception | the exception (`throwing = "error"`) |
| `@After` | after either (like `finally`) | nothing extra |
| `@Around` | instead of the method | everything |

`AuditAspect` writes a success or failure entry to [`AuditTrail`](../../src/main/java/jbdl/session09/aop/AuditTrail.java).
It can't swallow the exception: the caller still gets it (test `auditOnFailure`).

## 3. Retrying with `@Around`: [`RetryAspect`](../../src/main/java/jbdl/session09/aop/RetryAspect.java)

An around advice may call `proceed()` **more than once**. [`PaymentGateway.charge`](../../src/main/java/jbdl/session09/orders/PaymentGateway.java)
is marked `@Retryable(maxAttempts = 3)`; the aspect retries only `TransientFailureException`. Real log
from the tests:

```
INFO  [-] jbdl.session09.aop.RetryAspect - PaymentGateway.charge(..) attempt 1 failed (payment provider timed out), retrying
INFO  [-] jbdl.session09.aop.RetryAspect - PaymentGateway.charge(..) attempt 2 failed (payment provider timed out), retrying
INFO  [-] jbdl.session09.orders.OrderService - order 1 placed for Asha (total 50.00)
...
WARN  [-] jbdl.session09.aop.RetryAspect - PaymentGateway.charge(..) failed after 3 attempts: payment provider timed out
```

Retry only **idempotent** operations (Session 8, topic 3): retrying "charge order 1" is safe if the
provider charges each order once; retrying "charge this card 500 rupees" could charge twice.

## 4. Tracing with `@Before` and a named pointcut: [`ServiceTraceAspect`](../../src/main/java/jbdl/session09/aop/ServiceTraceAspect.java)

```java
@Pointcut("within(@org.springframework.stereotype.Service jbdl.session09..*) && execution(public * *(..))")
void publicServiceMethods() { }
```

Read it as: *inside a class annotated `@Service` in `jbdl.session09` or a sub-package (`..`), any
public method with any return type (`*`), any name (`*`), any arguments (`..`)*. No annotation on
the methods needed. It logs at DEBUG:

```
DEBUG [checkout-42] j.session09.aop.ServiceTraceAspect - -> OrderService.place(..) [OrderRequest(customer=Asha, ...)]
```

**Beginner tip:** annotation pointcuts (`@annotation(Timed)`) are easier to read and to control than
`execution(...)` patterns. Start with those.

## Common pointcut expressions

| Expression | Matches |
|---|---|
| `@annotation(com.x.Timed)` | methods annotated `@Timed` |
| `execution(* com.x.OrderService.*(..))` | every method of `OrderService` |
| `execution(public * com.x..*Service.find*(..))` | public `find…` methods of classes ending in `Service`, in `com.x` and below |
| `within(com.x.orders..*)` | every method in that package and below |
| `bean(*Controller)` | every bean whose name ends with `Controller` (Spring only) |

## Tests

[`AspectsTest`](../../src/test/java/jbdl/session09/AspectsTest.java) (7 tests): proxy, audit on success
and failure, retry succeeds on the 3rd attempt, retry gives up after 3, timing and trace log lines
(captured with `OutputCaptureExtension`), and the self-invocation trap.

## Interview questions

- **Which advice can stop the method from running?** `@Around` (by not calling `proceed()`); `@Before` only by throwing.
- **What happens if two aspects match one method?** Both run; set the order with `@Order` on the aspect classes.
- **Can an aspect advise a private method?** Not with Spring AOP: proxies only see public calls from outside.
- **Why `throws Throwable` on an around advice?** `proceed()` can throw anything the method throws.
