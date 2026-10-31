package jbdl.airbnb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import jbdl.airbnb.bookings.BookingDtos.BookingRequest;
import jbdl.airbnb.bookings.BookingEvents.BookingConfirmed;
import jbdl.airbnb.bookings.BookingService;
import jbdl.airbnb.common.BusinessRuleException;
import jbdl.airbnb.listings.ListingDtos.ListingRequest;
import jbdl.airbnb.listings.ListingService;
import jbdl.airbnb.notifications.Notification;
import jbdl.airbnb.notifications.NotificationInbox;
import jbdl.airbnb.users.AppUser;
import jbdl.airbnb.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Session 22, topic 5: booking events and the host's notifications, and why the listener waits for
 * the COMMIT.
 */
@SpringBootTest
class BookingEventsTest {

    private static final LocalDate CHECK_IN = LocalDate.now().plusDays(30);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(3);

    @Autowired
    private BookingService bookings;
    @Autowired
    private ListingService listings;
    @Autowired
    private UserService users;
    @Autowired
    private NotificationInbox inbox;
    @Autowired
    private ApplicationEventPublisher events;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private AppUser host;
    private String guestEmail;
    private long flat;

    @BeforeEach
    void flat() {
        host = users.register("Asha", "asha-" + UUID.randomUUID() + "@example.com", "password-123");
        guestEmail = users.register("Ravi", "ravi-" + UUID.randomUUID() + "@example.com", "password-123").getEmail();
        flat = listings.create(host.getEmail(), new ListingRequest("Sea view flat", null, "Goa", 4, 2,
                new BigDecimal("3500.00"), Set.of())).id();
    }

    private BookingConfirmed confirmed() {
        return new BookingConfirmed(1, flat, "Sea view flat", host.getId(), "Test", CHECK_IN, CHECK_OUT,
                new BigDecimal("10500.00"));
    }

    @Test
    @DisplayName("A booking tells the host; a refused one tells nobody")
    void hostIsTold() {
        long bookingId = bookings.book(guestEmail, new BookingRequest(flat, CHECK_IN, CHECK_OUT, 2)).id();

        assertThat(inbox.of(host.getId())).singleElement()
                .extracting(Notification::message)
                .isEqualTo("Ravi booked Sea view flat from " + CHECK_IN + " to " + CHECK_OUT + " (10500.00 INR)");

        assertThatThrownBy(() -> bookings.book(guestEmail, new BookingRequest(flat, CHECK_IN, CHECK_OUT, 2)))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(inbox.of(host.getId())).hasSize(1);

        bookings.cancel(bookingId, guestEmail);
        assertThat(inbox.of(host.getId())).hasSize(2);
        assertThat(inbox.of(host.getId()).get(1).message()).startsWith("Ravi cancelled Sea view flat");
    }

    @Test
    @DisplayName("AFTER_COMMIT: an event published in a transaction that is ROLLED BACK reaches no one")
    void rolledBackTransactionIsNotAnnounced() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        // a transaction that publishes the event and then fails (rolled back)
        transaction.executeWithoutResult(status -> {
            events.publishEvent(confirmed());
            status.setRollbackOnly();
        });
        assertThat(inbox.of(host.getId())).isEmpty();

        // the same, committed: now the host is told
        transaction.executeWithoutResult(status -> events.publishEvent(confirmed()));
        assertThat(inbox.of(host.getId())).hasSize(1);
    }
}
