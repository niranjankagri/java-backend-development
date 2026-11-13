package jbdl.session04.parallel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Topic 5: only the deterministic facts about parallel streams are asserted (same sum, threads used,
 * order kept by forEachOrdered and toList). The unsafe shared-list version is never asserted:
 * its result changes every run, so it is shown in the demo main instead.
 */
class ParallelStreamsTest {

    @Test
    @DisplayName("Sequential and parallel give the same sum (stateless map, associative sum)")
    void sameResult() {
        assertThat(ParallelStreams.sumOfSquares(100_000, true)).isEqualTo(ParallelStreams.sumOfSquares(100_000, false));
        assertThat(ParallelStreams.sumOfSquares(3, false)).isEqualTo(1 + 4 + 9);
    }

    @Test
    @DisplayName("Sequential runs on the calling thread only")
    void sequentialUsesOneThread() {
        assertThat(ParallelStreams.threadsUsed(false)).containsExactly(Thread.currentThread().getName());
    }

    @Test
    @DisplayName("Parallel uses the common ForkJoinPool when the machine has more than one core")
    void parallelUsesCommonPool() {
        if (Runtime.getRuntime().availableProcessors() > 1) {
            // The calling thread also works on a parallel stream, and with tiny work items it can
            // sometimes finish everything before a pool thread starts (seen once in CI on 2 cores).
            // So run it a few times: at least one run must use the common pool.
            boolean poolUsed = IntStream.range(0, 20).anyMatch(attempt -> ParallelStreams.threadsUsed(true).stream()
                    .anyMatch(name -> name.startsWith("ForkJoinPool.commonPool")));
            assertThat(poolUsed).as("a common pool thread took part in at least one of 20 runs").isTrue();
        }
        assertThat(IntStream.range(0, 10).parallel().isParallel()).isTrue();
    }

    @Test
    @DisplayName("forEachOrdered and collect keep the source order in parallel")
    void orderPreserved() {
        assertThat(ParallelStreams.orderedOutput(1_000)).isEqualTo(IntStream.range(0, 1_000).boxed().toList());
        assertThat(ParallelStreams.collectSafely(1_000)).isEqualTo(IntStream.range(0, 1_000).boxed().toList());
    }
}
