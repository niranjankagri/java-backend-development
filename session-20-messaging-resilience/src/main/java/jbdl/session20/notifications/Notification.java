package jbdl.session20.notifications;

/** One message to a customer. A real service would send it as an e-mail or SMS. */
public record Notification(String eventId, long orderId, String to, String message) {
}
