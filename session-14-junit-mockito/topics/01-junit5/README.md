# Topic 1: JUnit 5 testing

A **unit test** is a small program that calls your code with known input and checks the output. JUnit
runs the tests and reports which pass. `./mvnw test` runs them all (Session 6: the surefire plugin).

## A test, and its parts

```java
class JUnitBasicsTest {

    private Book book;

    @BeforeEach                      // runs before EVERY test: a fresh Book each time
    void setUp() {
        book = new Book(1, "Clean Code", 2);
    }

    @Test                            // a test method: no parameters, no return value
    @DisplayName("assertEquals: expected value first, actual second")
    void takeCopy() {
        book.takeCopy();                              // when
        assertEquals(1, book.getCopiesAvailable());   // then
    }
}
```

## The annotations used in [`JUnitBasicsTest`](../../src/test/java/jbdl/session14/JUnitBasicsTest.java)

| Annotation | Meaning |
|---|---|
| `@Test` | A test method |
| `@DisplayName("...")` | Readable name in reports instead of the method name |
| `@BeforeEach` / `@AfterEach` | Run before / after every test (fresh objects, cleanup) |
| `@BeforeAll` / `@AfterAll` | Run once per class (`static`): expensive setup |
| `@ParameterizedTest` + `@CsvSource` | One method, run once per row of data |
| `@Nested` | Group tests of one situation ("when the last copy is borrowed") |
| `@Tag("fast")` | Label; run a subset with `./mvnw test -Dgroups=fast` |
| `@Disabled("reason")` | Skip a test (always give a reason) |
| `@Timeout(5)` | Fail if it takes longer (used in Session 5) |

## Assertions

| Assertion | Checks |
|---|---|
| `assertEquals(expected, actual)` | equal values (`equals`) |
| `assertTrue(condition)` / `assertFalse` | a boolean |
| `assertThrows(Type.class, () -> code)` | the code throws that exception; returns it to check the message |
| `assertAll(() -> ..., () -> ...)` | runs every check and reports **all** failures at once |
| `assumeTrue(condition)` | if false, the test is **skipped**, not failed |

The other test classes use **AssertJ** (`assertThat(x).isEqualTo(y)`), included with Spring Boot: it
reads like a sentence and gives clearer failure messages. Both are fine; pick one style per project.

## Good tests are FIRST

| | |
|---|---|
| **F**ast | milliseconds, so you run them all the time |
| **I**ndependent | no test depends on another's leftovers (`@BeforeEach` gives fresh objects) |
| **R**epeatable | same result every time: no real clock, network or random data (Topic 3 fixes the clock) |
| **S**elf-validating | pass or fail, no reading of printed output |
| **T**imely | written with the code, not months later |

## Run them

```bash
./mvnw -pl session-14-junit-mockito test                         # all 23 tests
./mvnw -pl session-14-junit-mockito test -Dtest=JUnitBasicsTest   # one class
./mvnw -pl session-14-junit-mockito test -Dgroups=fast            # by tag
```

## Interview questions

- **JUnit 4 vs 5?** JUnit 5 (Jupiter): `@BeforeEach` instead of `@Before`, `assertThrows`, nested and parameterized tests built in, extensions instead of runners.
- **`@BeforeEach` vs `@BeforeAll`?** Per test vs once per class (static).
- **What makes a test flaky?** Hidden dependencies: time, order, shared state, network.
- **Assumption vs assertion?** A failed assumption skips the test; a failed assertion fails it.
