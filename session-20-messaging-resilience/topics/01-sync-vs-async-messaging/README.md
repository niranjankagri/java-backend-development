# Topic 1: Synchronous calls vs. asynchronous messaging

In Session 19 order-service **called** catalog-service and waited for the answer. That is
synchronous communication. This session adds the other kind: order-service **publishes a message**
(an event) and carries on, and whoever is interested reads it later.

[`OrderService.place`](../../src/main/java/jbdl/session20/orders/OrderService.java) uses both:

```java
shipping = shippingFees.quote(request.city());          // synchronous: the order needs the fee NOW
...
events.publish(new OrderPlaced(eventId, order.id(), ...)); // asynchronous: nobody has to answer
```

## The difference

| | Synchronous call (HTTP, Feign) | Asynchronous message (Kafka) |
|---|---|---|
| The caller | waits for the answer | continues at once |
| The other service is down | the call fails (needs timeout, retry, fallback: topics 4 and 5) | the message waits in the broker until it is back |
| Who receives it | exactly one service, which the caller knows | any number of services; the sender does not know them |
| Good for | questions that need an answer now ("what does shipping cost?") | facts others react to ("an order was placed") |
| Costs | tight coupling in time: both must be up | a broker to run, and results arrive later ("eventually") |

## Commands and events

- A **command** asks one service to do something: "send this e-mail". The sender expects it to happen.
- An **event** reports something that already happened: "order 7 was placed" (past tense). The
  sender does not care who reacts. Adding a new reaction (say an analytics service) needs **no change
  in order-service**: the new service just starts reading the topic.

`OrderPlaced` is an event. notification-service reacts by writing a message; order-service does not
know it exists.

## Delivery guarantees

A message system can promise one of:

- **at most once**: never twice, but can be lost
- **at least once**: never lost, but can arrive twice (Kafka's usual setting, used here)
- **exactly once**: possible in Kafka only in narrow cases (transactions between Kafka topics)

With at least once, every consumer must cope with repeats: it must be **idempotent** (handling the
same event twice has the same effect as once). notification-service remembers event ids (topic 2).

## What it looks like

Stop notification-service, place an order, start notification-service again: the order works, and
the message is written as soon as the service is back.

```
$ curl -X POST localhost:8200/orders -H 'Content-Type: application/json' -d '{"customerEmail":"meera@example.com","city":"Haldwani","amount":200}'
{"id":7,"customerEmail":"meera@example.com","city":"Haldwani","amount":200,"shippingFee":80,"shippingFeeSource":"shipping-service","total":280}

(notification-service started again)
OrderPlaced 304249d6-6f9e-44e9-8b74-63cb0ddc69d3 for order 7: message written
```

## Interview questions

- **When would you use a message instead of a REST call?** When the sender does not need an answer,
  when several services react to the same fact, or when the receiver may be down or slow and the work
  can wait.
- **What is eventual consistency?** After an order, the notification exists a moment later, not at
  the same instant. The system becomes consistent, but not immediately.
- **At-least-once delivery: what must the consumer do?** Be idempotent: detect repeats (by an event
  id) or make the operation naturally repeatable.
