# Java Backend Development - Spring Boot, Microservices with AI

[![Tests](https://github.com/niranjankagri/java-backend-development/actions/workflows/tests.yml/badge.svg)](https://github.com/niranjankagri/java-backend-development/actions/workflows/tests.yml)

My work for the Java backend course (JBDL, Java Backend Development Live), session by session: from
core Java to Spring Boot, microservices and AI tools. Every topic has a README; topics with code
explain it and link to the classes, and every example is checked by tests.

One session is added per day, each in its own pull request.

## Sessions

| # | Session | Status |
|---|---|---|
| 1 | [Java Architecture and Basics of OOPs](session-01-java-architecture-oop/README.md) | Done |
| 2 | [Advanced Java Concepts](session-02-advanced-java/README.md) | Done |
| 3 | [Java 8 Functional Programming & Collections Overview](session-03-functional-programming-collections/README.md) | Done |
| 4 | [Java Advanced Features](session-04-java-advanced-features/README.md) | Done |
| 5 | [Java Concurrency Essentials](session-05-concurrency/README.md) | Done |
| 6 | [Maven Project Management](session-06-maven/README.md) | Done |
| 7 | [Maven Plugins](session-07-maven-plugins/README.md) | Done |
| 8 | [Spring MVC Basics & REST API Implementation](session-08-spring-mvc-rest/README.md) | Done |
| 9 | [Spring Boot Advanced Features](session-09-spring-boot-advanced/README.md) | Done |
| 10 | [Java Database Connectivity (JDBC)](session-10-jdbc/README.md) | Done |
| 11 | [Hibernate, Spring Data JPA](session-11-jpa/README.md) | Done |
| 12 | [Minor Project: Splitwise Backend (Part 1)](session-12-splitwise-part-1/README.md) | Done |
| 13 | [Minor Project: Splitwise Backend (Continued)](session-13-splitwise-part-2/README.md) | Done |
| 14 | [Unit Testing with JUnit & Mockito](session-14-junit-mockito/README.md) | Done |
| 15 | [Redis & Caching](session-15-redis-caching/README.md) | Done |
| 16 | [Spring Security & Authentication](session-16-spring-security/README.md) | Done |
| 17 | [Splitwise Backend: Advanced Enhancements](session-17-splitwise-advanced/README.md) | Done |
| 18 | [OAuth 2 & GitHub Integration](session-18-oauth2-github/README.md) | Done |
| 19 | [Microservices](session-19-microservices/README.md) | Done |
| 20 | Messaging & Resilience | Planned |
| 21 | Airbnb Backend System (Part 1) | Planned |
| 22 | Airbnb Backend System (Part 2) | Planned |
| 23 | Deployment of Airbnb Backend Application | Planned |
| 24 | Deployment of Airbnb Backend Application (Continued) | Planned |
| 25 | The AI Toolkit & Developer Setup | Planned |
| 26 | Prompt Engineering & Vibe Coding | Planned |
| 27 | Spec-Driven Development with AI | Planned |
| 28 | RAG Systems & Context-Aware AI | Planned |

## Layout

```
pom.xml                          parent: Java version, dependency versions, compiler + test settings
session-NN-<name>/
  README.md                      the session: topics, what you can do after it, how to run
  topics/NN-<topic>/README.md    one README per topic; topics with code explain it and link to it
  pom.xml, src/                  only for sessions with code (each is a Maven module)
splitwise-backend/               the Splitwise project module, grown in Sessions 12, 13 and 17
.github/workflows/tests.yml      CI: build + all tests on JDK 17 and 25 for every pull request
```

## Run it

Needs Java 17 or newer. Maven itself is not needed: the Maven Wrapper downloads a checked version.

```bash
./mvnw verify                                       # every session: compile (warnings are errors) + tests
./mvnw -pl session-01-java-architecture-oop verify  # one session
```

On Windows use `mvnw.cmd`.

## The 2024 code

My original 2024 exercises are kept unchanged in the history: commit
[`4b8754b`](https://github.com/niranjankagri/java-backend-development/tree/4b8754b). They were upgraded
(tests, fixes) and now live in the session they belong to:

| 2024 file(s) | Now | What changed |
|---|---|---|
| `Car.java`, `Quiz.java`, `Main.java`, `inheritance/*` | Session 1 | Builder for the 7-property car; quiz answers tested; `SuperCar.getInfo()` builds on `super.getInfo()`; factory |
| `functionalinterface/*`, `collections/*` | Session 3 | Collections examples ignored every return value; `FunctionalInterface` hid `java.lang.FunctionalInterface`; duplicate `MetroEngine2` merged |
| `multithreading/*` | Session 5 | **Bugs fixed:** `ThreadJoin` never started its worker thread; `RunnableInterface` never used its `Runnable` |

## License

MIT, see [LICENSE](LICENSE).
