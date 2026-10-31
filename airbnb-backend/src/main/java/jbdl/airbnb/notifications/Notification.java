package jbdl.airbnb.notifications;

import java.time.Instant;

/** A message for a user ("Ravi booked Sea view flat ..."). A real app would e-mail or push it. */
public record Notification(String message, Instant createdAt) {
}
