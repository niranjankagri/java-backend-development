# Topic 4: Load balancing

## Why

One catalog instance can serve only so many requests, and when it stops (crash, new deployment)
every order fails. Run **several** instances and spread the calls: more capacity, and no single point
of failure.

## Server-side vs. client-side

- **Server-side**: a load balancer in front of the instances (Nginx, a cloud load balancer,
  a Kubernetes Service). Callers know one address.
- **Client-side**: the caller knows the list of instances and picks one per call. That is
  **Spring Cloud LoadBalancer**, used here: Feign asks it for an instance of `catalog-service` on
  every call.

## Where do the instances come from?

Service discovery answers "where does `catalog-service` run right now?". In production that is a
registry (Eureka, Consul) the instances register with, or Kubernetes. This session uses the simplest
form, a fixed list ([`application-orders.properties`](../../src/main/resources/application-orders.properties)):

```properties
spring.cloud.discovery.client.simple.instances.catalog-service[0].uri=http://localhost:8191
spring.cloud.discovery.client.simple.instances.catalog-service[1].uri=http://localhost:8192
```

## Round robin

The default rule takes the instances in turn. Three orders, real output:

```
$ curl -X POST localhost:8190/orders -H 'Content-Type: application/json' -d '{"productId":2,"quantity":1}'
{"id":1,"productId":2,"productName":"Mouse","quantity":1,"unitPrice":19.50,"total":19.50,"pricedBy":"catalog-1"}
$ ... "quantity":2 ...
{"id":2,"productId":2,"productName":"Mouse","quantity":2,"unitPrice":19.50,"total":39.00,"pricedBy":"catalog-2"}
$ ... "quantity":3 ...
{"id":3,"productId":2,"productName":"Mouse","quantity":3,"unitPrice":19.50,"total":58.50,"pricedBy":"catalog-1"}
```

(Spring Cloud LoadBalancer also has a random rule; others, like "fewest active requests", are
written as your own `ReactorServiceInstanceLoadBalancer`.)

## When an instance stops

A fixed list does not notice a stopped instance. The first version of this session had no health
check; after stopping catalog-2, **every second order failed**:

```
{"id":4,...,"pricedBy":"catalog-1"} [201]
{"detail":"The catalog service is not available right now. Try again later.",...,"status":503,...} [503]
{"id":5,...,"pricedBy":"catalog-1"} [201]
{"detail":"The catalog service is not available right now. Try again later.",...,"status":503,...} [503]
```

The fix: let the load balancer check each instance's `/actuator/health` (topic 2) and skip the ones
that are not `UP`:

```properties
spring.cloud.loadbalancer.configurations=health-check
spring.cloud.loadbalancer.health-check.interval=3s
```

The health check needs an HTTP client: a `RestClient` bean (in
[`OrderServiceApplication`](../../src/main/java/jbdl/session19/orders/OrderServiceApplication.java)).
Without it the health check is silently not set up and the load balancer finds **no** instances
("No servers available for service: catalog-service"); that was the second surprise while building
this session.

With the health check, real output: stop catalog-2, and after at most one check interval all orders
go to catalog-1:

```
catalog-2 stopped
[503]
"pricedBy":"catalog-1" [201]
[503]
after 4 s:
"pricedBy":"catalog-1" [201]
"pricedBy":"catalog-1" [201]
"pricedBy":"catalog-1" [201]
```

The few failures in the window before the next check are why Session 20 adds **retries** (try the
next instance) and **circuit breakers**.

## Tested

[`MicroservicesEndToEndTest`](../../src/test/java/jbdl/session19/orders/MicroservicesEndToEndTest.java)
starts two catalog instances and order-service on free ports, all in the test:

- `loadBalancing`: eight orders are spread over catalog-1 and catalog-2, both take some. Not always
  exactly 4 + 4: the health check (every 200 ms in the test) hands the load balancer a fresh instance
  list, in either order, so a turn can land on the same instance twice. The test first expected an
  exact 2 + 2 of four orders and failed about one run in three; it now checks what round robin with
  health checks really promises.
- `failover`: close catalog-2; after the next health check (200 ms in the test) every order is
  priced by catalog-1.
- `catalogDown`: both instances closed: `503 Catalog unavailable`, not `500`.

## Interview questions

- **Client-side vs. server-side load balancing?** Client-side: the caller picks the instance (no extra
  hop, needs discovery in every client). Server-side: a proxy picks (callers stay simple, one more
  component).
- **What is service discovery?** A live list of the instances of each service: registry (Eureka,
  Consul) or the platform (Kubernetes DNS / Services).
- **Why health checks?** To stop sending traffic to dead or not-yet-ready instances.
- **Round robin's weakness?** It ignores how busy or slow an instance is; one slow instance still gets
  its full share.
