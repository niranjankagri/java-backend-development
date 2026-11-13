# Topic 2: Architecture and project setup

## One application, not microservices (yet)

Sessions 19 and 20 split a shop into services. For this project we start with **one** Spring Boot
application, a *modular monolith*:

- the team is small and the domain is still changing; one codebase is easier to change;
- a booking must check the listing and the dates **in one database transaction** (requirement N1).
  Across two services that needs messaging and compensation, which is much harder to get right;
- one deployment, one database, one log.

The parts are still kept apart, by **package**, so they could become services later if one part
needs to scale or change on its own:

```
jbdl.airbnb
  users       AppUser, registration
  listings    Listing, the host's listing API, search
  bookings    Booking, DateRange (the stay rule); the booking API comes in Session 22
  common      exceptions and the error answers shared by all parts
```

A part talks to another part through its **service**, not its repository: `ListingService` asks
`UserService.get(hostId)` for the host and does not read the user table itself.

## Layers, as in Splitwise

```
HTTP ──▶ Controller ──▶ Service ──▶ Repository ──▶ database
         (JSON, @Valid)  (rules,       (Spring Data JPA)
                          @Transactional)
```

- **Controllers** are thin: they turn HTTP into method calls and back
  ([`ListingController`](../../../airbnb-backend/src/main/java/jbdl/airbnb/listings/ListingController.java)).
- **Services** hold the rules and the transactions
  ([`ListingService`](../../../airbnb-backend/src/main/java/jbdl/airbnb/listings/ListingService.java)).
- **DTOs** (records) are what goes over HTTP; entities stay inside
  ([`ListingDtos`](../../../airbnb-backend/src/main/java/jbdl/airbnb/listings/ListingDtos.java)).
- Errors become Problem Details in one place
  ([`ApiExceptionHandler`](../../../airbnb-backend/src/main/java/jbdl/airbnb/common/ApiExceptionHandler.java)):
  400 invalid input (with every wrong field or parameter), 403 not your listing, 404 unknown id,
  409 business rule.

## Project setup

The module [`airbnb-backend`](../../../airbnb-backend) is one more Maven module of the course
([`pom.xml`](../../../airbnb-backend/pom.xml)): Spring Web MVC, Validation, Data JPA, and H2 as an
in-memory database for development and tests. PostgreSQL and Docker arrive with deployment
(Sessions 23 and 24), as they did for Splitwise in Session 17.

[`application.properties`](../../../airbnb-backend/src/main/resources/application.properties):

```properties
spring.datasource.url=jdbc:h2:mem:airbnb
spring.jpa.hibernate.ddl-auto=create-drop     # tables from the entities (topic 3)
spring.jpa.open-in-view=false                 # no database access while the JSON is written
```

`open-in-view=false`: every service method loads what its answer needs inside its transaction. With
the default (`true`), lazy relations could still be loaded while Jackson writes the JSON: hidden
extra queries, outside any transaction.

## Who is calling? (until Session 22)

There is no login yet. Requests that change a listing carry `hostId`, and the service checks that it
is the listing's host (403 otherwise). That is **not** security: anyone can send any id. Session 22
replaces it with the logged-in user (Spring Security, Session 16), and the check stays where it is.

## Run it

```bash
./mvnw -pl airbnb-backend package
java -jar airbnb-backend/target/airbnb-backend-1.0.0-SNAPSHOT.jar      # port 8080
```

## Interview questions

- **Monolith or microservices for a new product?** Usually a well-structured monolith first; split
  when a part has a real reason (scale, team, release speed). Consistency rules like "no double
  bookings" are far simpler inside one database.
- **What is a modular monolith?** One deployable application whose parts are separated by package and
  talk through clear interfaces, so they could be split out later.
- **Why DTOs instead of returning entities?** The API stays stable when the tables change, nothing
  internal leaks, and no lazy-loading surprises during JSON writing.
