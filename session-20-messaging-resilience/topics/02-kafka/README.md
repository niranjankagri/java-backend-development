# Topic 2: Apache Kafka with Spring Boot

## The words

| Word | Meaning | Here |
|---|---|---|
| **Broker** | the Kafka server that stores messages | one broker in Docker (`localhost:9092`) |
| **Topic** | a named stream of messages | `orders.placed` |
| **Partition** | a topic is split into partitions; order is kept **inside** a partition | 3 partitions ([`OrderServiceApplication`](../../src/main/java/jbdl/session20/orders/OrderServiceApplication.java)) |
| **Key** | decides the partition: same key, same partition, same order | the order id |
| **Offset** | a message's position in its partition | Kafka remembers per group "read up to here" |
| **Producer** | writes messages | order-service |
| **Consumer group** | the consumers that share the work of one topic: each message goes to **one** member | `notification-service` |

Unlike a classic queue, Kafka does **not** delete a message once it is read. It keeps messages for a
time (7 days by default), and each consumer group has its own offset. A second group (say
`analytics`) reads every event too, independently, even old ones.

## Producer: KafkaTemplate

[`KafkaOrderEvents`](../../src/main/java/jbdl/session20/orders/KafkaOrderEvents.java):

```java
String key = String.valueOf(event.orderId());     // all events of an order -> same partition
kafka.send(TOPIC, key, event).join();              // wait until the broker has stored it
```

The event is sent as JSON
([`application-orders.properties`](../../src/main/resources/application-orders.properties)):

```properties
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer
spring.kafka.producer.properties.spring.json.add.type.headers=false
```

The second line keeps Java class names **out** of the message: the JSON is the contract, and the
consumer has its own [`OrderPlaced`](../../src/main/java/jbdl/session20/notifications/OrderPlaced.java)
class (the producer/consumer idea of Session 19, topic 5).

## Consumer: @KafkaListener

[`OrderPlacedListener`](../../src/main/java/jbdl/session20/notifications/OrderPlacedListener.java):

```java
@KafkaListener(topics = "orders.placed")
public void on(OrderPlaced event) {
    boolean written = notifications.handle(event);
    ...
}
```

Spring Kafka polls the broker on its own thread, turns the JSON into `OrderPlaced`
([`application-notifications.properties`](../../src/main/resources/application-notifications.properties):
`JacksonJsonDeserializer` with a default type), calls the method, and commits the offset afterwards.

`auto-offset-reset=earliest`: a group that has never read the topic starts at the oldest stored
message. After that the committed offset decides: a restarted notification-service continues where
it stopped, it does not read everything again.

## Idempotent consumer

Kafka delivers at least once (topic 1), so
[`NotificationService`](../../src/main/java/jbdl/session20/notifications/NotificationService.java)
remembers the event ids it has handled:

```java
if (!handledEventIds.add(event.eventId())) {   // add() is false when the id was already there
    return false;                               // a repeat: no second message to the customer
}
```

In production this memory is a database table with the event id as its primary key, so it survives
restarts and works with several instances.

## When the listener fails

If `on()` throws, Spring Kafka's `DefaultErrorHandler` delivers the same event again (10 attempts in
total, no pause), then logs it and moves on, so one bad event does not block the partition forever.
In production, failed events are usually kept in a **dead-letter topic** to inspect and replay later.
This module does not include that; it is one bean:

```java
@Bean
DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
    // after 3 attempts 1 s apart, publish the event to "orders.placed-dlt" (Spring Kafka 4 naming)
    return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(1000, 2));
}
```

## Run it

Kafka in Docker (KRaft mode, no ZooKeeper; the same image the tests use):

```bash
docker run -d --name kafka -p 9092:9092 apache/kafka-native:4.1.1
```

then the services ([session README](../../README.md#run-it)). Two orders, and the messages
notification-service wrote from the two events (real run):

```
$ curl -X POST localhost:8200/orders -H 'Content-Type: application/json' -d '{"customerEmail":"asha@example.com","city":"Delhi","amount":450}'
{"id":1,"customerEmail":"asha@example.com","city":"Delhi","amount":450,"shippingFee":40,"shippingFeeSource":"shipping-service","total":490}
$ curl -X POST localhost:8200/orders -H 'Content-Type: application/json' -d '{"customerEmail":"ravi@example.com","city":"Mumbai","amount":600}'
{"id":2,"customerEmail":"ravi@example.com","city":"Mumbai","amount":600,"shippingFee":0,"shippingFeeSource":"free","total":600}

$ curl localhost:8201/notifications
[{"eventId":"22bc50b3-982a-41cc-98db-2f41ade2dd45","orderId":1,"to":"asha@example.com","message":"Hello asha@example.com! Thank you for order 1. Total: 490 INR."},
 {"eventId":"72d6bd09-544f-4461-ac96-5f46f55bbf2a","orderId":2,"to":"ravi@example.com","message":"Hello ravi@example.com! Thank you for order 2. Total: 600 INR."}]
```

notification-service's log:

```
OrderPlaced 22bc50b3-982a-41cc-98db-2f41ade2dd45 for order 1: message written
OrderPlaced 72d6bd09-544f-4461-ac96-5f46f55bbf2a for order 2: message written
```

## Tested

- [`NotificationServiceTest`](../../src/test/java/jbdl/session20/notifications/NotificationServiceTest.java):
  the message from the template, and the same event twice gives one message. No Kafka needed.
- [`OrderServiceTest`](../../src/test/java/jbdl/session20/orders/OrderServiceTest.java): every order
  publishes one event with a new event id (a lambda collects the events instead of Kafka).
- [`MessagingEndToEndTest`](../../src/test/java/jbdl/session20/MessagingEndToEndTest.java): a real
  Kafka broker in Docker (Testcontainers) and the real services. An order arrives as a message; the
  same event published twice gives one message (a third "marker" event with the same key shows that
  both copies were read). Skipped without Docker, like Session 15's Redis tests.

## Interview questions

- **How does Kafka keep messages in order?** Only within a partition. Give related messages the same
  key (here the order id) so they go to the same partition.
- **Two instances of notification-service: does each get every event?** No. They are one consumer
  group, and the partitions are shared between them. A different group gets every event.
- **More consumers than partitions?** The extra consumers sit idle; the partition count is the
  maximum parallelism of a group.
- **What is an offset commit?** The group's note "read up to here". After a restart the consumer
  continues from it; committing after processing gives at-least-once delivery.
- **Kafka vs. RabbitMQ?** Kafka keeps a replayable log that many groups read independently; RabbitMQ
  routes messages to queues and removes them once acknowledged.
