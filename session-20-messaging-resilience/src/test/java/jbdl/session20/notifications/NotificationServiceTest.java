package jbdl.session20.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** NotificationService without Kafka: the message, and the idempotency rule (topic 2). */
class NotificationServiceTest {

    private final NotificationService service = new NotificationService("Hi %s, order %s is confirmed (total %s INR).");

    @Test
    @DisplayName("An event becomes one message, filled in from the template")
    void writesMessage() {
        boolean written = service.handle(new OrderPlaced("event-1", 7, "asha@example.com", new BigDecimal("490")));

        assertThat(written).isTrue();
        assertThat(service.sent()).singleElement().satisfies(notification -> {
            assertThat(notification.to()).isEqualTo("asha@example.com");
            assertThat(notification.message()).isEqualTo("Hi asha@example.com, order 7 is confirmed (total 490 INR).");
        });
    }

    @Test
    @DisplayName("The same event twice (Kafka delivers at least once): still one message")
    void idempotent() {
        OrderPlaced event = new OrderPlaced("event-1", 7, "asha@example.com", new BigDecimal("490"));

        service.handle(event);
        boolean writtenAgain = service.handle(event);

        assertThat(writtenAgain).isFalse();
        assertThat(service.sent()).hasSize(1);
    }

    @Test
    @DisplayName("A failed write is not remembered: the redelivered event is written then")
    void failedWriteIsRetried() {
        // a service whose first delivery fails, as a mail server that is down for a moment would
        NotificationService flaky = new NotificationService("Hi %s, order %s is confirmed (total %s INR).") {
            private boolean failedOnce;

            @Override
            protected void deliver(Notification notification) {
                if (!failedOnce) {
                    failedOnce = true;
                    throw new IllegalStateException("mail server down");
                }
                super.deliver(notification);
            }
        };
        OrderPlaced event = new OrderPlaced("event-1", 7, "asha@example.com", new BigDecimal("490"));

        assertThatThrownBy(() -> flaky.handle(event)).hasMessage("mail server down");
        boolean writtenOnRedelivery = flaky.handle(event);

        assertThat(writtenOnRedelivery).isTrue();
        assertThat(flaky.sent()).hasSize(1);
    }

    @Test
    @DisplayName("The same event from many threads at once: one message")
    void idempotentUnderConcurrency() throws Exception {
        OrderPlaced event = new OrderPlaced("event-1", 7, "asha@example.com", new BigDecimal("490"));
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return service.handle(event);
                }));
            }
            start.countDown();
            int written = 0;
            for (Future<Boolean> result : results) {
                written += result.get() ? 1 : 0;
            }
            assertThat(written).isEqualTo(1);
            assertThat(service.sent()).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("Two events of the same order with different event ids: two messages")
    void differentEvents() {
        service.handle(new OrderPlaced("event-1", 7, "asha@example.com", new BigDecimal("490")));
        service.handle(new OrderPlaced("event-2", 7, "asha@example.com", new BigDecimal("490")));

        assertThat(service.sent()).hasSize(2);
    }
}
