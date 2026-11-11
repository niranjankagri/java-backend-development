# Session 19: Microservices

From one application to several small services: when that is worth it, how two Spring Boot services
call each other with Feign, how the calls are spread over several instances, and how a producer and
its consumers keep their contract.

The example: **catalog-service** owns products and prices; **order-service** takes orders and asks
the catalog for the price, through a Feign client and a load balancer over two catalog instances.

| # | Topic | Code | README |
|---|---|---|---|
| 1 | Monolithic vs. microservices architecture | (README only) | [01-monolith-vs-microservices](topics/01-monolith-vs-microservices/README.md) |
| 2 | Developing Spring Boot microservices | [`catalog`](src/main/java/jbdl/session19/catalog), [`OrderServiceApplication`](src/main/java/jbdl/session19/orders/OrderServiceApplication.java), [`pom.xml`](pom.xml) | [02-spring-boot-microservices](topics/02-spring-boot-microservices/README.md) |
| 3 | Inter-service communication with Feign | [`CatalogClient`](src/main/java/jbdl/session19/orders/CatalogClient.java), [`OrderService`](src/main/java/jbdl/session19/orders/OrderService.java) | [03-feign-client](topics/03-feign-client/README.md) |
| 4 | Load balancing | [`application-orders.properties`](src/main/resources/application-orders.properties), [`MicroservicesEndToEndTest`](src/test/java/jbdl/session19/orders/MicroservicesEndToEndTest.java) | [04-load-balancing](topics/04-load-balancing/README.md) |
| 5 | The consumer and producer model | [`CatalogProduct`](src/main/java/jbdl/session19/orders/CatalogProduct.java), [`CatalogContractTest`](src/test/java/jbdl/session19/catalog/CatalogContractTest.java) | [05-consumer-and-producer](topics/05-consumer-and-producer/README.md) |

## What you can do after this session

- Say when microservices are worth their cost and when a (modular) monolith is the better choice.
- Build a Spring Boot service with a narrow job and run several instances of it.
- Call another service through a Feign interface, with timeouts and clear 404 / 503 handling.
- Spread calls over instances with Spring Cloud LoadBalancer and skip stopped instances.
- Name producer and consumer, keep an API compatible, and check the contract in a test.
- Test a service alone, and test several real services together in one JUnit test.

## Run it

```bash
./mvnw -pl session-19-microservices verify          # 13 tests, incl. 3 real services talking over HTTP
```

Then start two catalog instances and order-service (three terminals; Spring Cloud 2025.1, Boot 4.1):

```bash
cd session-19-microservices
java -jar target/session-19-microservices-1.0.0-SNAPSHOT-catalog.jar --server.port=8191 --catalog.instance=catalog-1
java -jar target/session-19-microservices-1.0.0-SNAPSHOT-catalog.jar --server.port=8192 --catalog.instance=catalog-2
java -jar target/session-19-microservices-1.0.0-SNAPSHOT-orders.jar
```

```
$ curl -X POST localhost:8190/orders -H 'Content-Type: application/json' -d '{"productId":2,"quantity":1}'
{"id":1,"productId":2,"productName":"Mouse","quantity":1,"unitPrice":19.50,"total":19.50,"pricedBy":"catalog-1"}
$ curl -X POST localhost:8190/orders -H 'Content-Type: application/json' -d '{"productId":2,"quantity":2}'
{"id":2,"productId":2,"productName":"Mouse","quantity":2,"unitPrice":19.50,"total":39.00,"pricedBy":"catalog-2"}
```

Stop one catalog instance (Ctrl+C) and keep ordering: within 3 seconds every order goes to the other
one ([topic 4](topics/04-load-balancing/README.md#when-an-instance-stops)).
