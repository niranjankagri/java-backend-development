package jbdl.session09.orders;

import java.math.BigDecimal;
import jbdl.session09.aop.Retryable;
import jbdl.session09.aop.Timed;
import jbdl.session09.aop.TransientFailureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * A pretend payment provider that is flaky on purpose: the next N calls fail with a transient error
 * (set by failNextCalls). The retry logic is NOT in here: @Retryable hands it to RetryAspect.
 */
@Slf4j
@Component
public class PaymentGateway {

    // Plain ints, protected by `synchronized` methods: this bean is shared by all request threads
    // (Session 5, topic 3). An AtomicInteger would also work; synchronized is easier to read.
    private int failuresLeft;
    private int calls;

    /** Charges the amount; idempotent per order id in a real provider, so retrying is safe. */
    @Timed
    @Retryable(maxAttempts = 3)
    public String charge(long orderId, BigDecimal amount) {
        countCall();
        if (shouldFailThisCall()) {
            throw new TransientFailureException("payment provider timed out");
        }
        log.info("charged {} for order {}", amount, orderId);
        return "PAY-" + orderId;
    }

    private synchronized void countCall() {
        calls++;
    }

    /** True while there are planned failures left; uses one up each time. */
    private synchronized boolean shouldFailThisCall() {
        if (failuresLeft > 0) {
            failuresLeft--;
            return true;
        }
        return false;
    }

    /** Test/demo switch: make the next `count` calls fail. */
    public synchronized void failNextCalls(int count) {
        failuresLeft = count;
    }

    /** How many times charge() really ran (the retries included). */
    public synchronized int calls() {
        return calls;
    }

    public synchronized void reset() {
        failuresLeft = 0;
        calls = 0;
    }
}
