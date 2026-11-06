# Session 14: Unit Testing with JUnit & Mockito

A small library-loans service (borrow a book, give it back, pay a fine) written to be tested, and its
23 tests at every level of the test pyramid.

| # | Topic | Tests | README |
|---|---|---|---|
| 1 | JUnit 5 testing | [`JUnitBasicsTest`](src/test/java/jbdl/session14/JUnitBasicsTest.java) | [01-junit5](topics/01-junit5/README.md) |
| 2 | The Mockito framework | [`LoanServiceTest`](src/test/java/jbdl/session14/LoanServiceTest.java) | [02-mockito](topics/02-mockito/README.md) |
| 3 | Service-layer testing | [`LoanServiceTest`](src/test/java/jbdl/session14/LoanServiceTest.java) | [03-service-layer-testing](topics/03-service-layer-testing/README.md) |
| 4 | MockMvc | [`LoanControllerTest`](src/test/java/jbdl/session14/LoanControllerTest.java), [`LibraryApplicationTest`](src/test/java/jbdl/session14/LibraryApplicationTest.java) | [04-mockmvc](topics/04-mockmvc/README.md) |
| 5 | Controller-layer testing | [`LoanControllerTest`](src/test/java/jbdl/session14/LoanControllerTest.java) | [05-controller-layer-testing](topics/05-controller-layer-testing/README.md) |

The code under test: [`loans`](src/main/java/jbdl/session14/loans): `LoanService` (rules), `LibraryRepository`
(interface) + `InMemoryLibraryRepository`, `Notifier`, `LoanController`, and an injected `Clock`.

## What you can do after this session

- Write JUnit 5 tests with lifecycle methods, assertions, parameterized and nested tests.
- Replace collaborators with Mockito mocks: stub answers, verify calls, capture arguments, check order.
- Design services for testability (constructor injection, interfaces, an injected clock).
- Test controllers with MockMvc and `@WebMvcTest`, and place each test at the right level of the pyramid.

## Run it

```bash
./mvnw -pl session-14-junit-mockito test
java -jar session-14-junit-mockito/target/session-14-junit-mockito-1.0.0-SNAPSHOT.jar   # after package
curl -X POST localhost:8080/api/loans -H 'Content-Type: application/json' -d '{"bookId": 1, "member": "Asha"}'
```
