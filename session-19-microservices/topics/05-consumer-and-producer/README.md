# Topic 5: The consumer and producer model

## Who is who

Every interaction between services has two sides:

- the **producer** (also: provider) offers data or an API: **catalog-service** produces product data;
- the **consumer** uses it: **order-service** consumes product data.

A service is often both: order-service consumes the catalog and produces orders for others (an
invoice service, a website).

## Synchronous and asynchronous

| | Request/response (this session) | Messaging (Session 20) |
|---|---|---|
| How | consumer calls the producer's HTTP API and waits | producer publishes an event to a broker (Kafka); consumers read it when they can |
| Coupling in time | both must be up at the same moment | producer and consumer run independently |
| Who knows whom | consumer knows the producer's API | neither knows the other, only the topic |
| Typical use | "what is the price now?" (needs an answer) | "an order was placed" (others react later) |

## The contract

What the producer sends is a **contract**: consumers depend on its shape. Here it is
[`Product`](../../src/main/java/jbdl/session19/catalog/Product.java) on the producer's side:

```json
{"id":2,"name":"Mouse","price":19.50,"stock":40,"servedBy":"catalog-1"}
```

The consumer has its **own** class,
[`CatalogProduct`](../../src/main/java/jbdl/session19/orders/CatalogProduct.java), with only the
fields it reads: `id`, `name`, `price`, `servedBy`. It does not reuse the producer's class: sharing
classes between services through a common jar couples their releases again.

### Changes that are safe, and ones that are not

| Producer change | Effect on consumers |
|---|---|
| add a field (`"category"`) | safe, if consumers ignore unknown fields |
| remove or rename a field | breaks every consumer reading it |
| change a type (`price` as string) | breaks consumers |
| new endpoint, new optional parameter | safe |

**Tolerant reader**: the consumer reads only what it needs and ignores the rest. `CatalogProduct` has
no `stock`, and Spring Boot's JSON mapper ignores unknown fields, so the catalog can add fields
freely. For breaking changes, the producer offers a new version (`/v2/products`) next to the old one
until all consumers have moved.

## Checking the contract

The danger: the catalog team renames `price` to `unitPrice`, updates its own tests, all green;
order-service breaks in production. A **contract test** on the producer's side catches it
([`CatalogContractTest`](../../src/test/java/jbdl/session19/catalog/CatalogContractTest.java)):

- every field order-service reads is present with the right JSON type;
- the consumer's own class `CatalogProduct` can read the producer's **real** answer, extra field
  `stock` included.

Across separate repositories this is done with **consumer-driven contract** tools: the consumer
writes down what it expects (Pact, Spring Cloud Contract), and the producer's build checks it.

## Interview questions

- **Producer vs. consumer?** The producer offers the API or publishes events; the consumer calls or
  subscribes. Most services are both.
- **When synchronous, when asynchronous?** Synchronous when the caller needs the answer now;
  asynchronous for notifying others and for work that may happen later.
- **How do you change an API without breaking consumers?** Only additive changes; tolerant readers;
  new versions for breaking changes; contract tests in the producer's build.
- **Should services share DTO classes?** Usually not: it couples their releases. Each side owns its
  model of the contract.
