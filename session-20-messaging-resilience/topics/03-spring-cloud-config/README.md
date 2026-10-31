# Topic 3: Spring Cloud Config

With several services, every service has its own `application.properties`, often with the same values
(Kafka address, timeouts) and with business settings someone wants to change without a new build.
A **config server** keeps them in one place; each service asks it at start-up.

## The server

[`ConfigServerApplication`](../../src/main/java/jbdl/session20/config/ConfigServerApplication.java) is a
normal Spring Boot app with one extra annotation:

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication { ... }
```

Where the settings come from
([`application-config-server.properties`](../../src/main/resources/application-config-server.properties)):

```properties
server.port=8888
spring.cloud.config.server.native.search-locations=classpath:/config-repo/
```

The `native` profile reads plain files: one file per service, named after the service, in
[`config-repo`](../../src/main/resources/config-repo):

```
config-repo/order-service.properties          orders.free-shipping-from=500, orders.fallback-shipping-fee=99
config-repo/notification-service.properties   notifications.template=Hello %s! Thank you for order %s. ...
```

In production the source is usually a **Git repository** (`spring.cloud.config.server.git.uri`): a
settings change is then a reviewed commit with history, and rolling it back is a revert.

The server's answer for order-service (real run):

```
$ curl localhost:8888/order-service/default
{"name":"order-service","profiles":["default"],"label":null,"version":null,"state":null,
 "propertySources":[{"name":"classpath:/config-repo/order-service.properties",
                     "source":{"orders.free-shipping-from":"500","orders.fallback-shipping-fee":"99"}}]}
```

The URL is `/{application}/{profile}`; a file `order-service-prod.properties` would be added for the
`prod` profile on top of `order-service.properties`.

## The client

Every service that wants its settings from the server has the `spring-cloud-starter-config`
dependency and one line
([`application-orders.properties`](../../src/main/resources/application-orders.properties)):

```properties
spring.config.import=optional:configserver:http://localhost:8888
```

- `configserver:` asks the server for the settings of `spring.application.name` (`order-service`).
- `optional:` means: if the server is not reachable, start anyway with the local values. Without it
  the service refuses to start, which is the safer choice when a wrong local value could do harm.

Then the settings are used like any other, with `@Value`
([`OrderService`](../../src/main/java/jbdl/session20/orders/OrderService.java)):

```java
public OrderService(ShippingFees shippingFees, OrderEvents events,
                    @Value("${orders.free-shipping-from}") BigDecimal freeShippingFrom) {
```

## Who wins: the server or the local file?

order-service's own file has `orders.free-shipping-from=1000`, config-server has `500`. Settings
loaded through `spring.config.import` take precedence over the file that imports them, so **the
server wins** while it is reachable, and 1000 is the fallback when it is not.
[`ConfigServerTest`](../../src/test/java/jbdl/session20/config/ConfigServerTest.java) checks both:
with the server the service sees 500 and 99, without it 1000 and 150.

## One trap of this course module

In a real project config-server and each service are separate projects. Here they share one module
(one classpath), and that showed two things the hard way, both fixed in the services' `builder()`
([`OrderServiceApplication`](../../src/main/java/jbdl/session20/orders/OrderServiceApplication.java)):

```java
return new SpringApplicationBuilder(OrderServiceApplication.class)
        .profiles("orders")
        .properties("spring.application.name=order-service", "spring.cloud.config.enabled=true");
```

1. The config client needs `spring.application.name` **before** it reads the profile's file. With the
   name only in `application-orders.properties`, the client asked the server for "application" and
   got nothing back: the service started silently with its local values. Found by `ConfigServerTest`
   (it expected 500 and got 1000).
2. The config-server library switches the config client off (`spring.cloud.config.enabled=false`) in
   every application on its classpath, so `configserver:` was not even understood ("Incorrect
   ConfigDataLocationResolver chosen").

In a normal service project neither applies: the name sits in its own `application.properties`.

## Changing a setting

Change the file (or commit to the Git repository), then restart the service: it reads the new value
at start-up. Spring Cloud can also reload settings in a running service (`@RefreshScope` beans and
the `/actuator/refresh` endpoint, or Spring Cloud Bus to tell all instances at once); that is not used
here.

## Tested

[`ConfigServerTest`](../../src/test/java/jbdl/session20/config/ConfigServerTest.java) starts the real
config-server on a free port: it serves `order-service.properties`; order-service started with it
uses the server's values; started without it (optional import) it uses its own. The end-to-end test
([`MessagingEndToEndTest`](../../src/test/java/jbdl/session20/MessagingEndToEndTest.java)) runs all
services against the config-server, so its message template and fallback fee come from there.

## Interview questions

- **Why a config server?** One place for shared and changeable settings, the same values for all
  instances, history and review through Git, and no rebuild for a settings change.
- **Should secrets go there?** Not in plain text. The config server can encrypt values (`{cipher}...`),
  or secrets live in a vault (HashiCorp Vault, a cloud secret manager) and the server or platform
  passes them on.
- **The config server is down: what happens?** With `optional:` services start with local values;
  without it they fail at start-up. Running services are not affected; they read settings at start.
- **Kubernetes?** ConfigMaps and Secrets often take this role there, without a config server.
