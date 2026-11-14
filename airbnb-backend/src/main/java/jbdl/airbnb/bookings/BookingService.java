package jbdl.airbnb.bookings;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jbdl.airbnb.bookings.BookingDtos.BookingRequest;
import jbdl.airbnb.bookings.BookingDtos.BookingView;
import jbdl.airbnb.bookings.BookingEvents.BookingCancelled;
import jbdl.airbnb.bookings.BookingEvents.BookingConfirmed;
import jbdl.airbnb.common.BusinessRuleException;
import jbdl.airbnb.common.ForbiddenException;
import jbdl.airbnb.common.NotFoundException;
import jbdl.airbnb.listings.Listing;
import jbdl.airbnb.listings.ListingService;
import jbdl.airbnb.users.AppUser;
import jbdl.airbnb.users.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Booking and cancelling stays: the rules of Session 22, topics 1 and 2. */
@Service
public class BookingService {

    static final int MAX_NIGHTS = 30;

    private final BookingRepository bookings;
    private final ListingService listings;
    private final UserService users;
    private final ApplicationEventPublisher events;

    public BookingService(BookingRepository bookings, ListingService listings, UserService users,
                          ApplicationEventPublisher events) {
        this.bookings = bookings;
        this.listings = listings;
        this.users = users;
        this.events = events;
    }

    /**
     * Books a stay for the logged-in guest. Everything happens in ONE transaction, and the listing is
     * locked first: a second booking of the same listing waits at lockForBooking until this one is
     * committed, and then its overlap check sees this booking. That is what prevents double bookings
     * (topic 2); without the lock both could check "free" at the same moment and both be saved.
     */
    @Transactional
    public BookingView book(String guestEmail, BookingRequest request) {
        AppUser guest = users.byEmail(guestEmail);
        DateRange stay = new DateRange(request.checkIn(), request.checkOut());   // 400: no dates or out before in
        if (stay.checkIn().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("checkIn must be today or later");
        }
        if (stay.nights() > MAX_NIGHTS) {
            throw new IllegalArgumentException("a booking can be at most " + MAX_NIGHTS + " nights");
        }

        Listing listing = listings.lockForBooking(request.listingId());          // 404, and the lock
        long listingId = listing.getId();
        if (!listing.isActive()) {
            throw new BusinessRuleException("listing " + listingId + " is not available");
        }
        if (listing.getHost().getId().equals(guest.getId())) {
            throw new BusinessRuleException("you cannot book your own listing");
        }
        if (request.guests() > listing.getMaxGuests()) {
            throw new BusinessRuleException("listing " + listingId + " takes at most " + listing.getMaxGuests() + " guests");
        }
        if (bookings.existsOverlapping(listingId, stay.checkIn(), stay.checkOut())) {
            throw new BusinessRuleException("listing " + listingId + " is already booked for some of these nights");
        }

        // the price is fixed now and stored (Session 21, topic 3): nights x today's nightly price
        BigDecimal total = listing.getNightlyPrice().multiply(BigDecimal.valueOf(stay.nights()));
        Booking booking = bookings.save(new Booking(listing, guest, stay.checkIn(), stay.checkOut(),
                request.guests(), total));

        // Published now, delivered to @TransactionalEventListener listeners only AFTER the commit (topic 5)
        events.publishEvent(new BookingConfirmed(booking.getId(), listingId, listing.getTitle(),
                listing.getHost().getId(), guest.getName(), stay.checkIn(), stay.checkOut(), total));
        return BookingView.of(booking);
    }

    /** Only the guest who booked, only a confirmed booking, and only before the check-in day. */
    @Transactional
    public BookingView cancel(long bookingId, String guestEmail) {
        Booking booking = getEntity(bookingId);
        if (!booking.getGuest().getEmail().equals(guestEmail)) {
            throw new ForbiddenException("only the guest who booked can cancel booking " + bookingId);
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("booking " + bookingId + " is already cancelled");
        }
        if (!booking.getCheckIn().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("a booking can only be cancelled before its check-in day");
        }
        booking.cancel();   // the dates are free again: search and booking only count CONFIRMED stays

        Listing listing = booking.getListing();
        events.publishEvent(new BookingCancelled(bookingId, listing.getId(), listing.getTitle(),
                listing.getHost().getId(), booking.getGuest().getName(), booking.getCheckIn(), booking.getCheckOut()));
        return BookingView.of(booking);
    }

    /** The logged-in user's bookings as a guest. */
    @Transactional(readOnly = true)
    public List<BookingView> myBookings(String guestEmail) {
        AppUser guest = users.byEmail(guestEmail);
        return views(bookings.findByGuestIdOrderByCheckInAsc(guest.getId()));
    }

    /** The bookings of a listing, for its host only (403 for anyone else). */
    @Transactional(readOnly = true)
    public List<BookingView> listingBookings(long listingId, String hostEmail) {
        listings.requireHost(listingId, hostEmail);
        return views(bookings.findByListingIdOrderByCheckInAsc(listingId));
    }

    /** For the other parts (reviews): the booking entity, or 404. */
    @Transactional(readOnly = true)
    public Booking getEntity(long bookingId) {
        return bookings.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("booking " + bookingId + " not found"));
    }

    private static List<BookingView> views(List<Booking> list) {
        List<BookingView> views = new ArrayList<>();
        for (Booking booking : list) {
            views.add(BookingView.of(booking));
        }
        return views;
    }
}
