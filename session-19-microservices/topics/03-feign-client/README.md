# Topic 3: Inter-service communication with Feign

## The client is an interface

order-service needs one thing from catalog-service: a product by id. With Spring Cloud OpenFeign
that is an interface, written like a controller, but for the calling side
([`CatalogClient`](../../src/main/java/jbdl/session19/orders/CatalogClient.java)):

```java
@FeignClient(name = "catalog-service")
public interface CatalogClient {

    @GetMapping("/products/{id}")
    CatalogProduct product(@PathVariable("id") long id);
}
```

`@EnableFeignClients` on
[`OrderServiceApplication`](../../src/main/java/jbdl/session19/orders/OrderServiceApplication.java)
makes Spring Cloud create a class for it. Calling `catalog.product(2)`:

1. asks the load balancer for an instance of **`catalog-service`** (a name, not an address; topic 4),
2. sends `GET http://localhost:8191/products/2`,
3. turns the JSON answer into a
   [`CatalogProduct`](../../src/main/java/jbdl/session19/orders/CatalogProduct.java) with Jackson,
4. throws a `FeignException` for 4xx/5xx answers and connection problems.

The same call written by hand with `RestClient` (Session 9) is short too, but the address,
URL building, error handling and load balancing would be yours to write in every client:

```java
restClient.get().uri("http://localhost:8191/products/{id}", id).retrieve().body(CatalogProduct.class);
```

## Errors are part of the design

[`OrderService`](../../src/main/java/jbdl/session19/orders/OrderService.java) turns Feign's
exceptions into its own, so nothing above it knows about Feign:

```java
try {
    return catalog.product(productId);
} catch (FeignException.NotFound e) {
    throw new ProductNotFoundException(productId);     // the catalog answered: no such product -> 404
} catch (FeignException e) {
    throw new CatalogUnavailableException(e);          // down, timeout, 5xx -> 503
}
```

The difference matters: "no such product" is the catalog **working** and saying no; the caller made a
mistake (404). "The catalog did not answer" is **our** dependency failing; the caller did nothing
wrong, and may try again later (503, not 500).

```
$ curl -i -X POST localhost:8190/orders -H 'Content-Type: application/json' -d '{"productId":99,"quantity":1}'
HTTP/1.1 404
{"detail":"Product 99 does not exist in the catalog","instance":"/orders","status":404,"title":"Product not found"}
```

## Timeouts: never wait forever

A slow catalog must not freeze order-service's threads. Per Feign client
([`application-orders.properties`](../../src/main/resources/application-orders.properties)):

```properties
spring.cloud.openfeign.client.config.catalog-service.connect-timeout=1000
spring.cloud.openfeign.client.config.catalog-service.read-timeout=2000
```

## Feign today

OpenFeign is still widely used and is what many existing services contain. Spring Framework 6+ also
has **HTTP interface clients** (`@HttpExchange` on an interface, backed by `RestClient`), the same
idea without Spring Cloud; Spring Cloud OpenFeign is in maintenance mode and points new projects
there. Knowing Feign makes HTTP interfaces trivial, and the other way round.

## Tested

- [`OrderServiceTest`](../../src/test/java/jbdl/session19/orders/OrderServiceTest.java):
  `CatalogClient` has one method, so a **lambda** is a complete fake catalog. No Spring, no HTTP.
- [`MicroservicesEndToEndTest`](../../src/test/java/jbdl/session19/orders/MicroservicesEndToEndTest.java):
  the real Feign client over real HTTP to two running catalog instances, including the 404 and 503
  cases.

## Interview questions

- **What does Feign do for you?** Generates the HTTP client from an annotated interface: URL building,
  JSON mapping, error decoding, and integration with the load balancer.
- **404 from the other service: what do you return?** Depends on meaning; usually map it to your own
  domain error (here 404 "product not found"). Never pass through raw upstream errors.
- **Why timeouts?** Without them a slow dependency ties up all request threads and the caller fails
  too (cascading failure).
