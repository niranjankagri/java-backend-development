package jbdl.airbnb.notifications;

import jbdl.airbnb.bookings.BookingEvents.BookingCancelled;
import jbdl.airbnb.bookings.BookingEvents.BookingConfirmed;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Tells hosts about their bookings. It reacts to BookingService's events, and BookingService does not
 * know it exists: adding a reaction (an e-mail, a Kafka message as in Session 20) needs no change there.
 *
 * AFTER_COMMIT: called only once the booking is really saved. If the transaction is rolled back (a
 * double booking found at the last moment, a database error), the host is never told about a booking
 * that does not exist. A plain @EventListener would be called at once, inside the transaction.
 */
@Component
public class HostNotifier {

    private final NotificationInbox inbox;

    public HostNotifier(NotificationInbox inbox) {
        this.inbox = inbox;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookingConfirmed event) {
        inbox.add(event.hostId(), event.guestName() + " booked " + event.listingTitle() + " from "
                + event.checkIn() + " to " + event.checkOut() + " (" + event.totalPrice() + " INR)");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookingCancelled event) {
        inbox.add(event.hostId(), event.guestName() + " cancelled " + event.listingTitle() + " from "
                + event.checkIn() + " to " + event.checkOut());
    }
}
