package jbdl.airbnb;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jbdl.airbnb.bookings.Booking;
import jbdl.airbnb.bookings.BookingDtos.BookingRequest;
import jbdl.airbnb.bookings.BookingRepository;
import jbdl.airbnb.bookings.BookingService;
import jbdl.airbnb.bookings.BookingStatus;
import jbdl.airbnb.common.BusinessRuleException;
import jbdl.airbnb.listings.ListingDtos.ListingRequest;
import jbdl.airbnb.listings.ListingService;
import jbdl.airbnb.users.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Session 22, topic 2: the requirement "no double bookings, even at the same moment" (N1 of Session 21).
 *
 * 10 guests book the same flat for the same nights, all at once: 10 threads wait at a "start line"
 * (CountDownLatch, Session 5) and are released together. Each call is its own transaction. Exactly one
 * booking may succeed; the other 9 must be told the nights are taken.
 */
@SpringBootTest
class DoubleBookingTest {

    private static final int GUESTS = 10;

    @Autowired
    private BookingService bookingService;
    @Autowired
    private ListingService listingService;
    @Autowired
    private UserService userService;
    @Autowired
    private BookingRepository bookings;

    @Test
    @DisplayName("10 guests book the same nights at the same moment: exactly 1 booking, 9 refusals")
    void onlyOneWins() throws Exception {
        String host = userService.register("Asha", "asha-" + UUID.randomUUID() + "@example.com", "password-123").getEmail();
        long flat = listingService.create(host, new ListingRequest("Sea view flat", null, "Goa", 4, 2,
                new BigDecimal("3500.00"), Set.of())).id();
        List<String> guests = new ArrayList<>();
        for (int i = 0; i < GUESTS; i++) {
            guests.add(userService.register("Guest " + i, "guest-" + UUID.randomUUID() + "@example.com", "password-123")
                    .getEmail());
        }
        BookingRequest sameNights = new BookingRequest(flat, LocalDate.now().plusDays(30), LocalDate.now().plusDays(33), 2);

        CountDownLatch startLine = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(GUESTS);
        List<Future<String>> results = new ArrayList<>();
        for (String guest : guests) {
            // each thread: wait at the start line, then book; the answer is "booked" or the refusal message
            Callable<String> attempt = () -> {
                startLine.await();
                try {
                    bookingService.book(guest, sameNights);
                    return "booked";
                } catch (BusinessRuleException e) {
                    return e.getMessage();
                }
            };
            results.add(pool.submit(attempt));
        }
        startLine.countDown();                     // go!

        int booked = 0;
        int refused = 0;
        for (Future<String> result : results) {
            String answer = result.get();          // waits for that thread; rethrows anything unexpected
            if (answer.equals("booked")) {
                booked++;
            } else {
                assertThat(answer).isEqualTo("listing " + flat + " is already booked for some of these nights");
                refused++;
            }
        }
        pool.shutdown();

        assertThat(booked).isEqualTo(1);
        assertThat(refused).isEqualTo(GUESTS - 1);
        int confirmedInDatabase = 0;
        for (Booking booking : bookings.findByListingIdOrderByCheckInAsc(flat)) {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                confirmedInDatabase++;
            }
        }
        assertThat(confirmedInDatabase).isEqualTo(1);
    }
}
