package jbdl.session20.notifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Writes one message per order. The important rule: Kafka delivers an event AT LEAST once, so the
 * same event can arrive twice (for example when this service stopped after handling it but before
 * Kafka noted that). Handling it twice must not send the customer two messages: this service is
 * IDEMPOTENT, it remembers the event ids it has handled and skips repeats.
 *
 * <p>The order matters: an event id is remembered only AFTER its message was written. Remembered
 * first, a failed write (say the e-mail server is down) would make Kafka's redelivery look like a
 * repeat, and the message would be lost for good.
 */
@Service
public class NotificationService {

    private final String template;
    // A Set that many threads may use at once (Session 5). The simpler, older way to write the same:
    //   Set<String> handled = Collections.synchronizedSet(new HashSet<>());
    // In production this memory would be a database table with the event id as primary key, written in
    // the same transaction as the message (or its outbox row), so both happen or neither does.
    private final Set<String> handledEventIds = ConcurrentHashMap.newKeySet();
    private final List<Notification> sent = new ArrayList<>();

    // the template comes from config-server when it runs (topic 3)
    public NotificationService(@Value("${notifications.template}") String template) {
        this.template = template;
    }

    /**
     * Check, write, remember: one step under a lock, so the same event arriving twice at the same moment
     * still gives one message. If writing throws, the id is not remembered, the exception goes back to
     * the Kafka listener, and the redelivered event is written then.
     *
     * @return true if a message was written, false if the event was a repeat
     */
    public synchronized boolean handle(OrderPlaced event) {
        if (handledEventIds.contains(event.eventId())) {
            return false;
        }
        String message = String.format(template, event.customerEmail(), event.orderId(), event.total());
        deliver(new Notification(event.eventId(), event.orderId(), event.customerEmail(), message));
        handledEventIds.add(event.eventId());
        return true;
    }

    /** Writes one message. A real service would send an e-mail here, which can fail. */
    protected void deliver(Notification notification) {
        synchronized (sent) {
            sent.add(notification);
        }
    }

    /** A copy of every message written so far, oldest first. */
    public List<Notification> sent() {
        synchronized (sent) {
            return List.copyOf(sent);
        }
    }
}
