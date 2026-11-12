package jbdl.session20.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
    @DisplayName("Two events of the same order with different event ids: two messages")
    void differentEvents() {
        service.handle(new OrderPlaced("event-1", 7, "asha@example.com", new BigDecimal("490")));
        service.handle(new OrderPlaced("event-2", 7, "asha@example.com", new BigDecimal("490")));

        assertThat(service.sent()).hasSize(2);
    }
}
