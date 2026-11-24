package jbdl.session14;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jbdl.session14.loans.Book;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Book.takeCopy() under concurrent borrowers. A race shows up only now and then, so the test repeats
 * the same situation many times: 8 threads wait at a latch and then all try to take one of 4 copies.
 */
class BookThreadSafetyTest {

    private static final int ROUNDS = 300;
    private static final int THREADS = 8;
    private static final int COPIES = 4;

    @Test
    @DisplayName("8 borrowers at once, 4 copies: exactly 4 get one, the shelf ends at 0, never below")
    void neverMoreLoansThanCopies() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            for (int round = 1; round <= ROUNDS; round++) {
                Book book = new Book(1, "Clean Code", COPIES);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Boolean>> tries = new ArrayList<>();
                for (int i = 0; i < THREADS; i++) {
                    tries.add(pool.submit(() -> {
                        start.await();             // all threads start together
                        try {
                            book.takeCopy();
                            return true;
                        } catch (IllegalStateException empty) {
                            return false;
                        }
                    }));
                }
                start.countDown();

                int taken = 0;
                for (Future<Boolean> attempt : tries) {
                    if (attempt.get()) {
                        taken++;
                    }
                }
                assertThat(taken).as("copies taken in round %d", round).isEqualTo(COPIES);
                assertThat(book.getCopiesAvailable()).as("copies left in round %d", round).isZero();
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
