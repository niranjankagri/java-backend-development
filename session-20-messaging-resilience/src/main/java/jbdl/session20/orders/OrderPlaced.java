package jbdl.session20.orders;

import java.math.BigDecimal;

/**
 * The event order-service publishes: "this order was placed". An event describes something that
 * HAS happened (past tense), and is sent as JSON:
 * {"eventId":"3f2a...","orderId":1,"customerEmail":"asha@example.com","total":540}
 *
 * @param eventId a new random id per event: consumers use it to notice the same event arriving twice
 */
public record OrderPlaced(String eventId, long orderId, String customerEmail, BigDecimal total) {
}
