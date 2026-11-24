package jbdl.session14;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import jbdl.session14.loans.Book;
import jbdl.session14.loans.LibraryRepository;
import jbdl.session14.loans.Loan;
import jbdl.session14.loans.LoanService;
import jbdl.session14.loans.Notifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The "at most 3 books" rule under concurrent borrows by ONE member. Mockito mocks are the wrong tool
 * here (they answer what they were told, not what other threads did), so the test uses a small
 * thread-safe repository with real state: one book with plenty of copies.
 */
class LoanLimitThreadSafetyTest {

    private static final int ROUNDS = 300;
    private static final int THREADS = 8;

    /** A thread-safe in-memory repository with one book of 100 copies. */
    static final class ConcurrentRepository implements LibraryRepository {

        private final Book book = new Book(1, "Clean Code", 100);
        private final Map<Long, Loan> loans = new ConcurrentHashMap<>();
        private final AtomicLong nextId = new AtomicLong(1);

        @Override
        public Optional<Book> findBook(long bookId) {
            return bookId == 1 ? Optional.of(book) : Optional.empty();
        }

        @Override
        public Optional<Loan> findLoan(long loanId) {
            return Optional.ofNullable(loans.get(loanId));
        }

        @Override
        public Loan saveLoan(long bookId, String member, LocalDate borrowedOn, LocalDate dueOn) {
            Loan loan = new Loan(nextId.getAndIncrement(), bookId, member, borrowedOn, dueOn);
            loans.put(loan.id(), loan);
            return loan;
        }

        @Override
        public void deleteLoan(long loanId) {
            loans.remove(loanId);
        }

        @Override
        public int countLoansOf(String member) {
            return (int) loans.values().stream().filter(loan -> loan.member().equals(member)).count();
        }
    }

    @Test
    @DisplayName("8 borrows by one member at once: exactly 3 succeed, never a 4th book")
    void neverMoreThanThreeBooks() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
        Notifier quiet = new Notifier() {
            @Override
            public void send(String member, String message) {
                // no output: 300 rounds would flood the log
            }
        };
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            for (int round = 1; round <= ROUNDS; round++) {
                ConcurrentRepository repository = new ConcurrentRepository();
                LoanService service = new LoanService(repository, quiet, clock);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Boolean>> tries = new ArrayList<>();
                for (int i = 0; i < THREADS; i++) {
                    tries.add(pool.submit(() -> {
                        start.await();             // all borrows start together
                        try {
                            service.borrow(1, "Asha");
                            return true;
                        } catch (IllegalStateException limitReached) {
                            return false;
                        }
                    }));
                }
                start.countDown();

                int borrowed = 0;
                for (Future<Boolean> attempt : tries) {
                    if (attempt.get()) {
                        borrowed++;
                    }
                }
                assertThat(borrowed).as("books borrowed in round %d", round).isEqualTo(LoanService.MAX_BOOKS_PER_MEMBER);
                assertThat(repository.countLoansOf("Asha")).as("loans stored in round %d", round)
                        .isEqualTo(LoanService.MAX_BOOKS_PER_MEMBER);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
