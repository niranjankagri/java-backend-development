package jbdl.airbnb;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import jbdl.airbnb.bookings.Booking;
import jbdl.airbnb.bookings.BookingRepository;
import jbdl.airbnb.listings.ListingRepository;
import jbdl.airbnb.users.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Session 21 topic 5: GET /api/listings/search (open to everyone, no login). Each test has its own
 * city, so listings of other tests never show up. The tests store bookings directly with
 * BookingRepository, to put them exactly where a search must notice them (and cancelled ones, and
 * back-to-back ones), without going through the booking rules.
 *
 * Dates are relative to today (search refuses check-in dates in the past), so the tests keep working
 * whatever day they run.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ListingSearchTest extends ApiTestSupport {

    // a stay of 3 nights, starting 30 days from today
    private static final LocalDate CHECK_IN = LocalDate.now().plusDays(30);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(3);

    @Autowired
    private BookingRepository bookings;
    @Autowired
    private ListingRepository listings;
    @Autowired
    private AppUserRepository users;

    private String city;
    private TestUser host;
    private TestUser guest;

    @BeforeEach
    void newCity() throws Exception {
        city = uniqueCity("Goa");
        host = registerUser("Asha");
        guest = registerUser("Ravi");
    }

    private ResultActions search(String query) throws Exception {
        return mvc.perform(get("/api/listings/search?city=" + city + query));
    }

    /** Stores a CONFIRMED booking of the listing from checkIn to checkOut. */
    private Booking book(long listingId, LocalDate checkIn, LocalDate checkOut) {
        return bookings.save(new Booking(listings.getReferenceById(listingId), users.getReferenceById(guest.id()),
                checkIn, checkOut, 2, new BigDecimal("1000.00")));
    }

    private String dates(LocalDate checkIn, LocalDate checkOut) {
        return "&checkIn=" + checkIn + "&checkOut=" + checkOut;
    }

    @Test
    @DisplayName("By city (any upper/lower case) and number of guests")
    void cityAndGuests() throws Exception {
        createListing(host, "Studio", city, 2, "1500.00");
        createListing(host, "Villa", city, 8, "9000.00");
        createListing(host, "Elsewhere", uniqueCity("Pune"), 8, "2000.00");

        mvc.perform(get("/api/listings/search?city=" + city.toUpperCase() + "&guests=4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalListings").value(1))
                .andExpect(jsonPath("$.listings[0].title").value("Villa"));
    }

    @Test
    @DisplayName("maxPrice: only listings up to that nightly price")
    void maxPrice() throws Exception {
        createListing(host, "Studio", city, 2, "1500.00");
        createListing(host, "Villa", city, 8, "9000.00");

        search("&maxPrice=2000")
                .andExpect(jsonPath("$.listings", hasSize(1)))
                .andExpect(jsonPath("$.listings[0].title").value("Studio"));
    }

    @Test
    @DisplayName("With dates: a listing with an overlapping booking is left out")
    void bookedListingLeftOut() throws Exception {
        long booked = createListing(host, "Booked flat", city, 4, "3000.00");
        createListing(host, "Free flat", city, 4, "3000.00");
        book(booked, CHECK_IN.plusDays(1), CHECK_IN.plusDays(5));   // overlaps the last 2 nights

        search(dates(CHECK_IN, CHECK_OUT))
                .andExpect(jsonPath("$.totalListings").value(1))
                .andExpect(jsonPath("$.listings[0].title").value("Free flat"));

        // without dates, both are found: availability only matters for a stay
        search("").andExpect(jsonPath("$.totalListings").value(2));
    }

    @Test
    @DisplayName("Back to back: a booking that ends on our check-in day does not block it")
    void backToBack() throws Exception {
        long listing = createListing(host, "Flat", city, 4, "3000.00");
        book(listing, CHECK_IN.minusDays(3), CHECK_IN);              // the previous guest leaves that morning
        book(listing, CHECK_OUT, CHECK_OUT.plusDays(2));              // the next guest arrives as we leave

        search(dates(CHECK_IN, CHECK_OUT)).andExpect(jsonPath("$.totalListings").value(1));
    }

    @Test
    @DisplayName("A CANCELLED booking does not block the dates")
    void cancelledBooking() throws Exception {
        long listing = createListing(host, "Flat", city, 4, "3000.00");
        Booking booking = book(listing, CHECK_IN, CHECK_OUT);
        booking.cancel();
        bookings.save(booking);

        search(dates(CHECK_IN, CHECK_OUT)).andExpect(jsonPath("$.totalListings").value(1));
    }

    @Test
    @DisplayName("A deactivated listing is never found")
    void deactivatedLeftOut() throws Exception {
        long listing = createListing(host, "Flat", city, 4, "3000.00");
        mvc.perform(delete("/api/listings/" + listing).with(as(host)));

        search("").andExpect(jsonPath("$.totalListings").value(0));
    }

    @Test
    @DisplayName("sort=price and pages: 5 listings, 2 per page, page 1 holds the 3rd and 4th cheapest")
    void sortAndPages() throws Exception {
        createListing(host, "E", city, 2, "5000.00");
        createListing(host, "A", city, 2, "1000.00");
        createListing(host, "C", city, 2, "3000.00");
        createListing(host, "B", city, 2, "2000.00");
        createListing(host, "D", city, 2, "4000.00");

        search("&sort=price&page=1&size=2")
                .andExpect(jsonPath("$.listings", hasSize(2)))
                .andExpect(jsonPath("$.listings[0].title").value("C"))
                .andExpect(jsonPath("$.listings[1].title").value("D"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalListings").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));

        search("&sort=price_desc&size=1").andExpect(jsonPath("$.listings[0].title").value("E"));
        search("&sort=newest&size=1").andExpect(jsonPath("$.listings[0].title").value("D"));  // created last
    }

    @Test
    @DisplayName("Wrong searches are 400: one date only, check-out before check-in, check-in in the past")
    void invalidDates() throws Exception {
        search("&checkIn=" + CHECK_IN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("checkIn and checkOut are both required"));
        search(dates(CHECK_OUT, CHECK_IN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("checkOut must be after checkIn"));
        search(dates(LocalDate.now().minusDays(1), CHECK_OUT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("checkIn must be today or later"));
        search("&checkIn=20-12-2026&checkOut=" + CHECK_OUT)          // not an ISO date
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Wrong parameters are 400: unknown sort, page size over 50, 0 guests")
    void invalidParameters() throws Exception {
        search("&sort=rating")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("sort must be newest, price or price_desc"));
        search("&size=51")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.size").value("must be less than or equal to 50"));
        search("&guests=0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.guests").value("must be greater than or equal to 1"));
    }
}
