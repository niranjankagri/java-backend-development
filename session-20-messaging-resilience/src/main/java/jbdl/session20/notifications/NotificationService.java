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
 */
@Service
public class NotificationService {

    private final String template;
    // A Set that many threads may use at once (Session 5). The simpler, older way to write the same:
    //   Set<String> handled = Collections.synchronizedSet(new HashSet<>());
    // In production this memory would be a database table with the event id as primary key.
    private final Set<String> handledEventIds = ConcurrentHashMap.newKeySet();
    private final List<Notification> sent = new ArrayList<>();

    // the template comes from config-server when it runs (topic 3)
    public NotificationService(@Value("${notifications.template}") String template) {
        this.template = template;
    }

    /** @return true if a message was written, false if the event was a repeat */
    public boolean handle(OrderPlaced event) {
        // add() returns false when the id was already in the set: we handled this event before
        if (!handledEventIds.add(event.eventId())) {
            return false;
        }
        String message = String.format(template, event.customerEmail(), event.orderId(), event.total());
        synchronized (sent) {
            sent.add(new Notification(event.eventId(), event.orderId(), event.customerEmail(), message));
        }
        return true;
    }

    /** A copy of every message written so far, oldest first. */
    public List<Notification> sent() {
        synchronized (sent) {
            return List.copyOf(sent);
        }
    }
}
