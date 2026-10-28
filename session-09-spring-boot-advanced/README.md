# Session 9: Spring Boot Advanced Features

An order desk API (`POST /api/orders`, `GET /api/orders/{id}`) whose timing, auditing, retries and
request-id logging are added by aspects and a filter, not written inside the business methods.

| # | Topic | Code | README |
|---|---|---|---|
| 1 | Introduction to Aspect-Oriented Programming | [`aop`](src/main/java/jbdl/session09/aop) | [01-aop-introduction](topics/01-aop-introduction/README.md) |
| 2 | Implement cross-cutting concerns using Spring AOP | [`aop`](src/main/java/jbdl/session09/aop): timing, audit, retry, trace | [02-cross-cutting-concerns-with-spring-aop](topics/02-cross-cutting-concerns-with-spring-aop/README.md) |
| 3 | Integrate Lombok in a Spring Boot application | [`orders`](src/main/java/jbdl/session09/orders) | [03-lombok](topics/03-lombok/README.md) |
| 4 | Overview of logging in Spring Boot applications | [`logging`](src/main/java/jbdl/session09/logging), [`application.properties`](src/main/resources/application.properties) | [04-logging](topics/04-logging/README.md) |

## What you can do after this session

- Explain aspects, join points, pointcuts, advice and proxies, and the self-invocation trap.
- Write `@Around`, `@Before`, `@AfterReturning` and `@AfterThrowing` advice driven by your own annotations.
- Use Lombok's `@Value`, `@Data`, `@Builder`, `@RequiredArgsConstructor` and `@Slf4j`, and know when a record is enough.
- Configure log levels and patterns, and trace one request through the logs with an MDC request id.

## Run it

```bash
./mvnw -pl session-09-spring-boot-advanced verify          # 16 tests
java -jar session-09-spring-boot-advanced/target/session-09-spring-boot-advanced-1.0.0-SNAPSHOT.jar
curl -i -X POST localhost:8080/api/orders -H 'Content-Type: application/json' -H 'X-Request-Id: demo-1' \
     -d '{"customer":"Asha","lines":[{"product":"pen","quantity":2,"unitPrice":25.00}]}'
# the console shows every line of that request tagged [demo-1]
```
