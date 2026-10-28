# Topic 2: Set up the project environment

## Tools

| Tool | Version here | Why |
|---|---|---|
| JDK | 17+ (CI runs 17 and 25) | Spring Boot 4 needs Java 17 or newer |
| Maven | via the wrapper (`./mvnw`) | Same Maven for everyone (Session 6) |
| IDE | IntelliJ IDEA / VS Code | Import as a Maven project |
| Database | H2 in memory (now), PostgreSQL in Docker (Session 17) | Start without installing anything |
| HTTP client | curl, Postman or IntelliJ's HTTP client | Try the API by hand |

## How the project was created

Like a Spring Initializr project (Session 7, topic 2), as one more module of this repository:

```
splitwise-backend/
  pom.xml                                   Boot BOM; starters: webmvc, validation, data-jpa; H2; test starters
  src/main/java/jbdl/splitwise/
    SplitwiseApplication.java               @SpringBootApplication
    users/    groups/    expenses/          one package per feature (topic 3)
    common/                                 exceptions + the error handler shared by all features
  src/main/resources/application.properties
  src/test/java/jbdl/splitwise/             schema test + API tests
```

[`pom.xml`](../../../splitwise-backend/pom.xml) dependencies and why each is there:

| Dependency | Gives |
|---|---|
| `spring-boot-starter-webmvc` | REST controllers, embedded Tomcat, JSON |
| `spring-boot-starter-validation` | `@NotBlank`, `@Email` on request bodies |
| `spring-boot-starter-data-jpa` | Entities, repositories, transactions (Session 11) |
| `h2` (runtime) | The in-memory database |
| `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test` | MockMvc, test slices |

[`application.properties`](../../../splitwise-backend/src/main/resources/application.properties):

```properties
spring.application.name=splitwise
spring.datasource.url=jdbc:h2:mem:splitwise
spring.jpa.hibernate.ddl-auto=create-drop     # tables from the entities, fine until we have real data
spring.jpa.open-in-view=false                 # no database access from controllers/JSON rendering
```

## Run it

```bash
./mvnw -pl splitwise-backend verify                                         # build + 9 tests
java -jar splitwise-backend/target/splitwise-backend-1.0.0-SNAPSHOT.jar     # http://localhost:8080
```

## Interview questions

- **Why start with an in-memory database?** No setup, fast tests; switch the URL and driver later.
- **What does `open-in-view=false` do?** Closes the database session when the service returns, so lazy loading can't happen silently during JSON rendering.
- **Why the Maven wrapper in a team?** Same build tool version everywhere, including CI.
