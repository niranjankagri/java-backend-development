package jbdl.session20.notifications;

import java.math.BigDecimal;

/**
 * notification-service's OWN copy of the OrderPlaced event: the consumer side of the contract, with
 * the fields this service reads (Session 19, topic 5). The JSON on the topic is the contract, not a
 * shared Java class.
 */
public record OrderPlaced(String eventId, long orderId, String customerEmail, BigDecimal total) {
}
