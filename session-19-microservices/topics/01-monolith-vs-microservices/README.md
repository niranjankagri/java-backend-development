# Topic 1: Monolithic vs. microservices architecture

## The monolith

Everything the Splitwise backend (Sessions 12, 13, 17) does lives in **one** application: users,
groups, expenses, balances. One codebase, one build, one jar, one database, one deployment.

```
            +------------------------------------------+
 client --> |  splitwise-backend.jar                   |
            |   users | groups | expenses | balances   | --> one database
            +------------------------------------------+
```

That is a good thing to start with:

- A call from "expenses" to "users" is a **method call**: fast, cannot get lost, one transaction.
- One thing to build, test, deploy and watch.
- Refactoring across modules is one IDE action.

## Microservices

The same features split into **small services**, each owning one business area, its own code and
its own data, talking over the network (HTTP, or messages in Session 20):

```
 client --> order-service ---HTTP---> catalog-service (x2)
               |                          |
            orders DB                 catalog DB
```

This session builds exactly that, small: [`catalog-service`](../../src/main/java/jbdl/session19/catalog)
owns the products and prices; [`order-service`](../../src/main/java/jbdl/session19/orders) owns the
orders and asks the catalog for prices.

## What you gain, what you pay

| | Monolith | Microservices |
|---|---|---|
| Deploy | all or nothing | each service on its own, any day |
| Scale | the whole app | only the busy service (two catalog instances here) |
| Teams | everyone in one codebase | one team per service, own release pace |
| Failure | a crash takes down everything | one service down, others keep working (if built for it) |
| Technology | one stack | each service may choose |
| Calls between parts | method calls | network calls: slow, can fail, need timeouts |
| Data | one database, joins and transactions | each service its own data; no joins across services |
| Testing, debugging | one process | many processes, logs and versions to line up |
| Operations | one app to run | service discovery, load balancing, monitoring, tracing ... |

The bottom half of the table is the price. It is worth paying when the **organisation** is large
(many teams blocking each other) or parts of the system have **very different load**. A small team
with a new product usually moves faster with a well-structured monolith ("modular monolith") and
splits later, along the module borders it already has.

## Rules that make microservices work

- **Split by business capability**, not by technical layer: "catalog", "orders", "payments", not
  "controllers service" and "database service".
- **Database per service.** No other service reads the catalog's tables; it asks the catalog's API.
  Otherwise a column rename in one service breaks another, and you have a distributed monolith.
- **Own your copy of what you need.** An [`Order`](../../src/main/java/jbdl/session19/orders/Order.java)
  stores the product name and price it was sold at, so showing an order later needs no catalog call,
  and a price change does not rewrite history.
- **Every remote call can fail.** Set timeouts, decide what the caller gets (here: `503`), and in
  Session 20 add retries and circuit breakers.
- **Independently deployable** means the API must stay compatible across versions (topic 5).

## Interview questions

- **When would you NOT use microservices?** Small team, young product, unclear domain borders: the
  operational cost outweighs the benefits. Start with a modular monolith.
- **What is a distributed monolith?** Services that must be deployed together because they share a
  database or call each other in long synchronous chains: all the costs, none of the benefits.
- **Why database per service?** Each service can change its schema freely; coupling goes through
  APIs that can be versioned.
- **How do you split a monolith?** Along business capabilities (bounded contexts), one piece at a
  time, routing traffic to the new service while the old code still exists (strangler fig pattern).
