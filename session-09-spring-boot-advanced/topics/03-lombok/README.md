# Topic 3: Integrate Lombok in a Spring Boot application

Lombok is an **annotation processor**: while `javac` compiles your class, Lombok adds the boring
code (getters, constructors, `equals`...) to it. Nothing of Lombok is needed at run time.

## Setup (see [`pom.xml`](../../pom.xml))

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>          <!-- compile time only, not in the jar (Session 6, scopes) -->
</dependency>
```

Since **JDK 23**, `javac` runs annotation processors only when they are listed explicitly, so the
compiler plugin also gets `<annotationProcessorPaths>` with Lombok. In your IDE, install the Lombok
plugin and enable annotation processing, or it will show red errors for the generated methods.

One build detail in this repository: the root POM fails the build on any compiler warning. With an
annotation processor running, `javac` warns *"No processor claimed any of these annotations"* about
Spring's annotations. That's harmless, so this module switches off just that one warning
(`-Xlint:all,-processing`).

## The annotations used here

| Annotation | Generates | Used on |
|---|---|---|
| `@Getter` / `@Setter` | getters / setters | (part of the two below) |
| `@Data` | getters, **setters**, `equals`/`hashCode`, `toString`, constructor for final fields | [`OrderRequest`](../../src/main/java/jbdl/session09/orders/OrderRequest.java) (a mutable request body) |
| `@Value` | all fields `private final`, getters, `equals`/`hashCode`, `toString`, all-args constructor; **no setters** | [`Order`](../../src/main/java/jbdl/session09/orders/Order.java) (immutable) |
| `@Builder` (+ `@Singular`) | `Order.builder().id(1).customer("Asha").line(a).line(b).build()` | `Order` |
| `@NoArgsConstructor`, `@AllArgsConstructor` | the named constructors | `OrderRequest` (Jackson needs the no-args one) |
| `@RequiredArgsConstructor` | a constructor for every `final` field: **constructor injection** with no code | `OrderService`, `OrderController`, `AuditAspect` |
| `@Slf4j` | `private static final org.slf4j.Logger log = LoggerFactory.getLogger(ThisClass.class);` | services, aspects |

### Before and after

Without Lombok, `OrderService`'s first lines would be:

```java
private static final Logger log = LoggerFactory.getLogger(OrderService.class);   // @Slf4j
private final PaymentGateway payments;

public OrderService(PaymentGateway payments) {                                    // @RequiredArgsConstructor
    this.payments = payments;
}
```

and `OrderRequest` would need two constructors, four getters, four setters, `equals`, `hashCode`
and `toString`: about 60 lines instead of 10.

## Lombok vs records

A Java **record** (Session 1) already gives an immutable class with constructor, accessors,
`equals`, `hashCode` and `toString`, without any library. Use records for simple immutable data.
Lombok is still useful for: builders, mutable beans (`@Data`), loggers (`@Slf4j`), constructor
injection (`@RequiredArgsConstructor`), and JPA entities (Session 11), which can't be records.

## Cautions

- `@Data` on JPA entities: its `equals`/`hashCode`/`toString` use all fields, including lazy
  relations: slow queries or infinite loops. Use `@Getter`/`@Setter` there instead.
- Generated code is invisible in the source: `delombok` shows it if you're curious.

## Tests

[`LombokTest`](../../src/test/java/jbdl/session09/LombokTest.java) (5 plain unit tests) checks that the
generated code exists: the builder, `equals`/`toString`, that every `Order` field is `private final`
with no setters, the setters of `@Data`, the `log` field and the single injection constructor.

## Interview questions

- **How does Lombok work?** An annotation processor that adds code during compilation.
- **`@Data` vs `@Value`?** Mutable with setters vs immutable without.
- **Why scope `provided`?** Needed only to compile; nothing of Lombok runs in production.
- **Lombok or records?** Records for plain immutable data; Lombok for builders, loggers, injection constructors, entities.
