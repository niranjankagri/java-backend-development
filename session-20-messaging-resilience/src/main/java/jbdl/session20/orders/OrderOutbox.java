package jbdl.session20.orders;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The outbox pattern: events that still have to go to Kafka.
 *
 * Without it, OrderService stored the order and then sent the event: two writes to two systems (a
 * "dual write"). When Kafka was down, the order was stored but its event never sent, and the customer
 * got an error for an order that existed. Now OrderService only adds the event here, next to the order,
 * and {@link #relay()} sends it on its own thread, again and again until Kafka has stored it.
 *
 * With a database (Session 11) the outbox is a table: the order row and the event row are written in
 * ONE transaction, so either both exist or neither, and the relay reads the table. Here, like the
 * orders, the outbox is kept in memory to keep the session about messaging; nothing can fail between
 * the two in-memory writes, but a restart loses both.
 *
 * The relay sends each event AT LEAST once: if Kafka stores an event but the answer is lost, it is sent
 * again. That is why every event has its own event id and notification-service ignores repeats
 * (topic 2, idempotent consumer).
 */
@Component
public class OrderOutbox {

    private static final Logger log = LoggerFactory.getLogger(OrderOutbox.class);

    private final OrderEvents events;
    // Only relay() takes events out, and only one relay() runs at a time (synchronized), so the event
    // it peeks at is the one it removes. add() needs no lock, so a request never waits for Kafka.
    private final Queue<OrderPlaced> pending = new ConcurrentLinkedQueue<>();

    public OrderOutbox(OrderEvents events) {
        this.events = events;
    }

    /** Remembers an event to be sent. Returns at once; the relay sends it. */
    public void add(OrderPlaced event) {
        pending.add(event);
    }

    /** The events not sent yet, oldest first (for tests and troubleshooting). */
    public List<OrderPlaced> pending() {
        return List.copyOf(pending);
    }

    /**
     * Sends the waiting events, oldest first, and returns how many were sent. Runs every
     * orders.outbox.relay-every-ms milliseconds, counted from the end of the previous run.
     *
     * At the first failure it stops and keeps that event (and every later one) for the next run: an
     * event is never skipped, so the events of one order still reach Kafka in the order they happened.
     */
    @Scheduled(fixedDelayString = "${orders.outbox.relay-every-ms:500}")
    public synchronized int relay() {
        int sent = 0;
        OrderPlaced next;
        while ((next = pending.peek()) != null) {
            try {
                events.publish(next);
            } catch (RuntimeException e) {
                log.warn("Kafka did not take event {} of order {}; {} event(s) wait for the next try: {}",
                        next.eventId(), next.orderId(), pending.size(), e.toString());
                break;
            }
            pending.remove();
            sent++;
        }
        return sent;
    }
}
