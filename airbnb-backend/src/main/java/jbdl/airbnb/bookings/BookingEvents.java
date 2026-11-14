package jbdl.airbnb.bookings;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Session 22, topic 5: what BookingService tells the rest of the application. Events are facts in the
 * past tense; the booking code does not know who listens (today: the host's notifications).
 * They carry plain values, not entities: a listener may run after the transaction and its entities
 * are gone.
 */
public final class BookingEvents {

    private BookingEvents() {
    }

    public record BookingConfirmed(long bookingId, long listingId, String listingTitle, long hostId,
                                   String guestName, LocalDate checkIn, LocalDate checkOut, BigDecimal totalPrice) {
    }

    public record BookingCancelled(long bookingId, long listingId, String listingTitle, long hostId,
                                   String guestName, LocalDate checkIn, LocalDate checkOut) {
    }
}
