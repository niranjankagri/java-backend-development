package jbdl.airbnb;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Session 22, topic 4: reviews. A review needs a stay that is over, and the booking API only books
 * future stays, so the tests store past stays directly with BookingRepository.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReviewApiTest extends ApiTestSupport {

    @Autowired
    private BookingRepository bookings;
    @Autowired
    private ListingRepository listings;
    @Autowired
    private AppUserRepository users;

    private TestUser asha;
    private TestUser ravi;
    private long flat;

    @BeforeEach
    void flatInGoa() throws Exception {
        asha = registerUser("Asha");
        ravi = registerUser("Ravi");
        flat = createListing(asha, "Sea view flat", uniqueCity("Goa"), 4, "3500.00");
    }

    /** A stay of the guest from daysAgoIn to daysAgoOut days ago (negative: in the future). */
    private Booking stay(TestUser guest, int daysAgoIn, int daysAgoOut) {
        return bookings.save(new Booking(listings.getReferenceById(flat), users.getReferenceById(guest.id()),
                LocalDate.now().minusDays(daysAgoIn), LocalDate.now().minusDays(daysAgoOut), 2,
                new BigDecimal("7000.00")));
    }

    private ResultActions review(TestUser user, long bookingId, int rating, String comment) throws Exception {
        return postJson("/api/bookings/" + bookingId + "/review",
                "{\"rating\": " + rating + ", \"comment\": \"" + comment + "\"}", user);
    }

    @Test
    @DisplayName("Two reviews after two stays: average 4.5 (one decimal), newest first, open to everyone")
    void reviewsAndAverage() throws Exception {
        long first = stay(ravi, 20, 18).getId();
        TestUser meera = registerUser("Meera");
        long second = stay(meera, 10, 7).getId();

        review(ravi, first, 5, "Lovely view").andExpect(status().isCreated())
                .andExpect(jsonPath("$.guestName").value("Ravi"));
        review(meera, second, 4, "Noisy at night").andExpect(status().isCreated());

        mvc.perform(get("/api/listings/" + flat + "/reviews"))          // no login
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.reviewCount").value(2))
                .andExpect(jsonPath("$.reviews", hasSize(2)))
                .andExpect(jsonPath("$.reviews[0].comment").value("Noisy at night"));
    }

    @Test
    @DisplayName("No reviews yet: averageRating null, count 0; unknown listing 404")
    void noReviews() throws Exception {
        mvc.perform(get("/api/listings/" + flat + "/reviews"))
                .andExpect(jsonPath("$.averageRating").doesNotExist())
                .andExpect(jsonPath("$.reviewCount").value(0));
        mvc.perform(get("/api/listings/999999/reviews")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Review rules: only the guest (403), only once (409), from the check-out day (409), not cancelled (409)")
    void rules() throws Exception {
        long past = stay(ravi, 5, 2).getId();
        review(asha, past, 5, "My own flat is great").andExpect(status().isForbidden());

        review(ravi, past, 5, "Lovely").andExpect(status().isCreated());
        review(ravi, past, 1, "Changed my mind")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("booking " + past + " already has a review"));

        long future = stay(ravi, -10, -12).getId();
        review(ravi, future, 5, "Can't wait")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("a stay can be reviewed from its check-out day"));

        long checkingOutToday = stay(ravi, 3, 0).getId();
        review(ravi, checkingOutToday, 4, "Nice").andExpect(status().isCreated());

        Booking cancelled = stay(ravi, 30, 28);
        cancelled.cancel();
        bookings.save(cancelled);
        review(ravi, cancelled.getId(), 3, "Never went")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("a cancelled booking cannot be reviewed"));
    }

    @Test
    @DisplayName("Rating outside 1 to 5: 400; without a login: 401")
    void invalid() throws Exception {
        long past = stay(ravi, 5, 2).getId();
        review(ravi, past, 6, "Too good").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rating").exists());
        mvc.perform(post("/api/bookings/" + past + "/review")).andExpect(status().isUnauthorized());
    }
}
