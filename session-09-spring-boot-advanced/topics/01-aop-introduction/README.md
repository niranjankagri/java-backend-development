# Topic 1: Introduction to Aspect-Oriented Programming (AOP)

## The problem

Some code has to run in **many** methods but has nothing to do with what those methods are for:
measuring time, writing an audit log, retrying a flaky call, checking permissions, opening a
transaction. These are **cross-cutting concerns**. Without AOP you copy the same lines everywhere:

```java
public Order place(OrderRequest request) {
    long start = System.nanoTime();                       // timing
    try {
        // ... the real work: 10 lines of business logic ...
        audit.record("ORDER_PLACED", order, true);        // auditing
        return order;
    } catch (RuntimeException e) {
        audit.record("ORDER_PLACED", e, false);           // auditing again
        throw e;
    } finally {
        log.info("place took {} us", (System.nanoTime() - start) / 1000);  // timing again
    }
}
```

…and the same in `cancel`, `refund`, `charge`. The business logic drowns, and forgetting it in one
method is easy.

## The AOP idea

Write the cross-cutting code **once**, in an **aspect**, and say **where** it applies. The business
method stays clean:

```java
@Timed
@Audited(action = "ORDER_PLACED")
public Order place(OrderRequest request) {
    // only the real work
}
```

## Words you need

| Term | Meaning | Example in this module |
|---|---|---|
| **Aspect** | A class holding cross-cutting code | `PerformanceAspect`, `AuditAspect`, `RetryAspect` |
| **Join point** | A point where an aspect can step in; in Spring AOP always a **method call** | the call to `OrderService.place(..)` |
| **Pointcut** | An expression that selects join points | `@annotation(timed)`: methods annotated `@Timed` |
| **Advice** | The code to run, and **when** | `@Around`, `@Before`, `@AfterReturning`, `@AfterThrowing`, `@After` |
| **Target** | The real object being advised | the `OrderService` instance |
| **Proxy** | The wrapper Spring puts around the target | what `@Autowired OrderService` really gives you |

## How Spring does it: proxies

```
caller ──▶ [ proxy:  PerformanceAspect ─▶ AuditAspect ─▶ ] ──▶ real OrderService.place()
           (Spring creates it at startup; it looks like an OrderService)
```

When another bean asks for an `OrderService`, Spring injects the **proxy**. Every call goes through
the aspects first. The test `beanIsAProxy` checks it: `AopUtils.isAopProxy(orders)` is `true`.

### The trap: calling yourself

Inside `OrderService`, `place(request)` means `this.place(request)`: a call on the **real** object,
not the proxy, so **no aspect runs**. `placeAll` does exactly that, and the test
`selfInvocationBypassesProxy` shows two orders placed but **zero** audit entries. Fixes: call the
method from another bean, or move the loop out of the class.

## Spring AOP vs AspectJ

| | Spring AOP (used here) | Full AspectJ |
|---|---|---|
| How | Proxies at run time | Changes the bytecode (compile or load time) |
| Join points | Public method calls on Spring beans | Also fields, constructors, private methods, self-calls |
| Setup | Nothing: `spring-boot-starter-aspectj` | A weaver (extra build step or agent) |

Spring borrows AspectJ's **annotations and pointcut language** but not its weaver. For almost every
application, Spring AOP is enough.

## Interview questions

- **What is a cross-cutting concern?** Logic needed across many modules but not part of their job: logging, security, transactions.
- **Join point vs pointcut vs advice?** Where it can apply / which ones to pick / what to run.
- **How does Spring AOP work?** With runtime proxies (JDK dynamic proxy for interfaces, CGLIB subclass otherwise).
- **Why doesn't my aspect run when a method calls another method of the same class?** Self-invocation bypasses the proxy.
- **Where does Spring itself use AOP?** `@Transactional`, `@Cacheable`, `@Async`, method security (`@PreAuthorize`).
