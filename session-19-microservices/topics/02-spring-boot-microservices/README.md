# Topic 2: Developing Spring Boot microservices

A microservice is just a Spring Boot application with a narrow job. Nothing new is needed to write
one; what is new is that there are several, and they must find and call each other.

## The two services

| | catalog-service | order-service |
|---|---|---|
| Owns | products and prices | orders |
| Main class | [`CatalogServiceApplication`](../../src/main/java/jbdl/session19/catalog/CatalogServiceApplication.java) | [`OrderServiceApplication`](../../src/main/java/jbdl/session19/orders/OrderServiceApplication.java) |
| API | `GET /products`, `GET /products/{id}` | `POST /orders`, `GET /orders/{id}` |
| Settings | [`application-catalog.properties`](../../src/main/resources/application-catalog.properties) | [`application-orders.properties`](../../src/main/resources/application-orders.properties) |
| Port | 8191 (and 8192 for a second instance) | 8190 |

Each has a `spring.application.name`: that is the service's **name** in logs, metrics and in
service discovery (topic 4).

## Two services in one Maven module

To keep the course one Maven project, both services live in the module `session-19-microservices`,
in separate packages. Three things keep them apart:

1. **Component scanning by package.** `@SpringBootApplication` scans its own package and below, so
   `CatalogServiceApplication` (in `jbdl.session19.catalog`) never sees order-service's beans and
   the other way round.
2. **A profile per service.** Each `main` starts with its profile:

   ```java
   new SpringApplicationBuilder(CatalogServiceApplication.class).profiles("catalog").run(args);
   ```

   so it loads `application-catalog.properties` (or `-orders`).
3. **Two jars.** The Spring Boot Maven plugin runs twice, each time with another main class and a
   classifier ([`pom.xml`](../../pom.xml)):

   ```
   target/session-19-microservices-1.0.0-SNAPSHOT-catalog.jar
   target/session-19-microservices-1.0.0-SNAPSHOT-orders.jar
   ```

In a real project each service is its **own** repository or Maven project, with its own
`application.properties`, its own pipeline and its own version. The idea is the same.

## Running several instances

The same jar runs as many times as you like; only the settings differ. Command-line arguments win
over the properties file:

```bash
java -jar target/session-19-microservices-1.0.0-SNAPSHOT-catalog.jar --server.port=8191 --catalog.instance=catalog-1
java -jar target/session-19-microservices-1.0.0-SNAPSHOT-catalog.jar --server.port=8192 --catalog.instance=catalog-2
```

```
$ curl localhost:8191/products/2
{"id":2,"name":"Mouse","price":19.50,"stock":40,"servedBy":"catalog-1"}
```

`servedBy` exists only so you can see which instance answered. Instances must be **stateless** (or
share their state through a database) for this to work: any instance may get any request.

## Health: is the service up?

Both services have Spring Boot Actuator. `GET /actuator/health` answers `{"status":"UP"}` while the
service is fine; load balancers, Kubernetes and monitoring use it (topic 4 uses it to skip stopped
catalog instances).

```
$ curl localhost:8191/actuator/health
{"groups":["liveness","readiness"],"status":"UP"}
```

## Tested on its own

[`CatalogControllerTest`](../../src/test/java/jbdl/session19/catalog/CatalogControllerTest.java)
starts **only** catalog-service (`@SpringBootTest(classes = CatalogServiceApplication.class)` +
`@ActiveProfiles("catalog")`) and checks its API with MockMvc, as in Session 8. Every service must be
testable without the others.

## Interview questions

- **How do several instances of one service differ?** Only in configuration (port, instance id),
  never in code; they must not keep user state in memory that another instance would need.
- **What is `spring.application.name` used for?** The service's identity: discovery, logs, metrics.
- **How do you configure many services?** Properties per service + environment variables /
  command-line arguments per instance; at scale a config server or Kubernetes ConfigMaps (Session 20).
