package jbdl.session20.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The Kafka consumer. Spring Kafka calls {@link #on} for every event on the topic, on its own
 * listener thread, and then commits the offset ("read up to here") so the event is not delivered again.
 */
@Component
public class OrderPlacedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedListener.class);

    private final NotificationService notifications;

    public OrderPlacedListener(NotificationService notifications) {
        this.notifications = notifications;
    }

    // If on() throws, Spring Kafka's default error handler tries the same event again (10 times in
    // total), then logs it and moves on; topic 2 shows how to send such events to a dead-letter topic.
    @KafkaListener(topics = "orders.placed")
    public void on(OrderPlaced event) {
        boolean written = notifications.handle(event);
        log.info("OrderPlaced {} for order {}: {}", event.eventId(), event.orderId(),
                written ? "message written" : "repeat, skipped");
    }
}
