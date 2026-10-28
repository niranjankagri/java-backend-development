package jbdl.session14;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jbdl.session14.loans.Book;
import jbdl.session14.loans.Loan;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Topic 1 (JUnit 5): the building blocks, on the two plain classes Book and Loan. Only JUnit's own
 * assertions (org.junit.jupiter.api.Assertions) are used here; the other test classes use AssertJ,
 * which reads more like a sentence.
 */
@DisplayName("JUnit 5 basics")
@Tag("fast") // run only these with: ./mvnw test -Dgroups=fast
class JUnitBasicsTest {

    // static: one list for the whole class, to show the order of the lifecycle methods
    private static final List<String> LIFECYCLE = new ArrayList<>();

    private Book book; // a fresh one for every test, created in @BeforeEach

    @BeforeAll
    static void beforeAll() {
        LIFECYCLE.add("beforeAll");      // once, before the first test of the class
    }

    @BeforeEach
    void setUp() {
        book = new Book(1, "Clean Code", 2); // before EACH test: tests never share state
        LIFECYCLE.add("beforeEach");
    }

    @AfterEach
    void tearDown() {
        LIFECYCLE.add("afterEach");      // after each test, e.g. to delete temporary files
    }

    @AfterAll
    static void afterAll() {
        // after the last test; the list starts with beforeAll and alternates beforeEach / afterEach
        assertEquals("beforeAll", LIFECYCLE.get(0));
    }

    @Test
    @DisplayName("assertEquals: expected value first, actual second")
    void takeCopy() {
        book.takeCopy();

        assertEquals(1, book.getCopiesAvailable());
    }

    @Test
    @DisplayName("assertThrows: checks the exception type and returns it to check the message")
    void noCopyLeft() {
        book.takeCopy();
        book.takeCopy();

        IllegalStateException e = assertThrows(IllegalStateException.class, book::takeCopy);
        assertEquals("no copies of \"Clean Code\" left", e.getMessage());
    }

    @Test
    @DisplayName("assertAll: runs every check and reports ALL failures, not just the first")
    void allAtOnce() {
        Loan loan = new Loan(7, 1, "Asha", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));

        assertAll(
                () -> assertEquals(7, loan.id()),
                () -> assertEquals("Asha", loan.member()),
                () -> assertEquals(LocalDate.of(2026, 10, 15), loan.dueOn()));
    }

    @ParameterizedTest(name = "returned on {0} -> {1} days late")
    @DisplayName("@ParameterizedTest: one test method, one run per row of data")
    @CsvSource({
            "2026-10-10, 0",     // early
            "2026-10-15, 0",     // on the due date: not late
            "2026-10-16, 1",
            "2026-10-25, 10"})
    void daysLate(LocalDate returnedOn, long expected) {
        Loan loan = new Loan(1, 1, "Ravi", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));

        assertEquals(expected, loan.daysLate(returnedOn));
    }

    @Test
    @DisplayName("Assumptions: skip (not fail) a test when a precondition doesn't hold")
    void onlyWithEnoughProcessors() {
        assumeTrue(Runtime.getRuntime().availableProcessors() >= 1, "needs at least one CPU");
        assertTrue(book.getCopiesAvailable() > 0);
    }

    /** @Nested: group related tests; the outer @BeforeEach still runs first. */
    @Nested
    @DisplayName("when the last copy is borrowed")
    class LastCopy {

        @BeforeEach
        void borrowAllButOne() {
            book.takeCopy(); // 1 left
        }

        @Test
        @DisplayName("the shelf becomes empty")
        void shelfEmpty() {
            book.takeCopy();
            assertEquals(0, book.getCopiesAvailable());
        }

        @Test
        @DisplayName("returning it puts it back")
        void returnIt() {
            book.takeCopy();
            book.returnCopy();
            assertFalse(book.getCopiesAvailable() == 0);
        }
    }
}
