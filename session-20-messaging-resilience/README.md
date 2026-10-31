# Session 20: Messaging & Resilience

Services that do not wait for each other (messages through Kafka), settings in one place (Spring
Cloud Config), and services that keep working when another one fails (timeouts, retries, circuit
breaker).

The example has four small services:

```
                      config-server (8888)
                  settings for order- and notification-service
                        │                     │
 POST /orders ──▶ order-service (8200) ── OrderPlaced ──▶ Kafka ──▶ notification-service (8201)
                        │                (topic orders.placed)       writes the customer a message
                        │ fee? (timeout, retry, circuit breaker, fallback)
                        ▼
                  shipping-service (8202)
```

| # | Topic | Code | README |
|---|---|---|---|
| 1 | Synchronous calls vs. asynchronous messaging | [`OrderService`](src/main/java/jbdl/session20/orders/OrderService.java) | [01-sync-vs-async-messaging](topics/01-sync-vs-async-messaging/README.md) |
| 2 | Apache Kafka: producer, consumer, idempotent consumer | [`KafkaOrderEvents`](src/main/java/jbdl/session20/orders/KafkaOrderEvents.java), [`OrderPlacedListener`](src/main/java/jbdl/session20/notifications/OrderPlacedListener.java), [`NotificationService`](src/main/java/jbdl/session20/notifications/NotificationService.java) | [02-kafka](topics/02-kafka/README.md) |
| 3 | Spring Cloud Config | [`ConfigServerApplication`](src/main/java/jbdl/session20/config/ConfigServerApplication.java), [`config-repo`](src/main/resources/config-repo) | [03-spring-cloud-config](topics/03-spring-cloud-config/README.md) |
| 4 | Circuit breaker and fallback | [`ResilienceConfig`](src/main/java/jbdl/session20/orders/ResilienceConfig.java), [`ShippingRates`](src/main/java/jbdl/session20/orders/ShippingRates.java) | [04-circuit-breaker](topics/04-circuit-breaker/README.md) |
| 5 | Retry and timeouts | [`ResilienceConfig`](src/main/java/jbdl/session20/orders/ResilienceConfig.java), [`ShippingRates`](src/main/java/jbdl/session20/orders/ShippingRates.java) | [05-retry-and-timeouts](topics/05-retry-and-timeouts/README.md) |

## What you can do after this session

- Choose between a synchronous call and an asynchronous event, and explain the trade-off.
- Publish events with `KafkaTemplate` and consume them with `@KafkaListener`, as JSON, with a key
  that keeps the events of one order in order.
- Explain topics, partitions, offsets and consumer groups, and why a consumer must be idempotent.
- Run a config server and let services take their settings from it, with a local fallback.
- Protect a remote call with a timeout, a retry and a circuit breaker, and give a visible fallback.
- Test all of it: with fakes and lambdas, with a fake HTTP server, and end to end with a real Kafka
  broker in Docker.

## Run it

```bash
./mvnw -pl session-20-messaging-resilience verify   # 22 tests; the 3 end-to-end tests need Docker
```

Then Kafka and the four services (five terminals, in this order):

```bash
docker run -d --name kafka -p 9092:9092 apache/kafka-native:4.1.1
cd session-20-messaging-resilience
java -jar target/session-20-messaging-resilience-1.0.0-SNAPSHOT-config.jar
java -jar target/session-20-messaging-resilience-1.0.0-SNAPSHOT-shipping.jar
java -jar target/session-20-messaging-resilience-1.0.0-SNAPSHOT-orders.jar
java -jar target/session-20-messaging-resilience-1.0.0-SNAPSHOT-notifications.jar
```

```
$ curl -X POST localhost:8200/orders -H 'Content-Type: application/json' -d '{"customerEmail":"asha@example.com","city":"Delhi","amount":450}'
{"id":1,"customerEmail":"asha@example.com","city":"Delhi","amount":450,"shippingFee":40,"shippingFeeSource":"shipping-service","total":490}
$ curl localhost:8201/notifications
[{"eventId":"22bc50b3-982a-41cc-98db-2f41ade2dd45","orderId":1,"to":"asha@example.com","message":"Hello asha@example.com! Thank you for order 1. Total: 490 INR."}]
```

Then try what the topics show:

- stop shipping-service (Ctrl+C) and keep ordering: the fallback fee, then the circuit opens
  (`curl localhost:8200/shipping/circuit-breaker`, [topic 4](topics/04-circuit-breaker/README.md#what-it-looks-like));
- stop notification-service, order, start it again: the message arrives late, but it arrives
  ([topic 1](topics/01-sync-vs-async-messaging/README.md#what-it-looks-like));
- stop config-server and restart order-service: free shipping from 1000 instead of 500, its local
  value ([topic 3](topics/03-spring-cloud-config/README.md#who-wins-the-server-or-the-local-file)).

Clean up: `docker rm -f kafka`.

Spring Boot 4.1, Spring Cloud 2025.1, Spring Kafka 4.1, Resilience4j 2.3, Kafka 4.1 (KRaft, no
ZooKeeper).
