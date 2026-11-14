package jbdl.airbnb;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Session 22, topic 1 through the HTTP API: booking, the booking rules, cancelling, and who may see
 * which bookings. Asha is the host of a flat (3,500 a night, up to 4 guests); Ravi and Meera are guests.
 * Dates are relative to today, so the tests keep working whatever day they run.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookingApiTest extends ApiTestSupport {

    private static final LocalDate CHECK_IN = LocalDate.now().plusDays(30);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(3);

    private TestUser asha;
    private TestUser ravi;
    private TestUser meera;
    private String city;
    private long flat;

    @BeforeEach
    void flatInGoa() throws Exception {
        asha = registerUser("Asha");
        ravi = registerUser("Ravi");
        meera = registerUser("Meera");
        city = uniqueCity("Goa");
        flat = createListing(asha, "Sea view flat", city, 4, "3500.00");
    }

    private ResultActions book(TestUser guest, long listingId, LocalDate checkIn, LocalDate checkOut, int guests)
            throws Exception {
        return postJson("/api/bookings", bookingJson(listingId, checkIn, checkOut, guests), guest);
    }

    private long bookingId(TestUser guest, LocalDate checkIn, LocalDate checkOut) throws Exception {
        return id(book(guest, flat, checkIn, checkOut, 2)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Book 3 nights: 201, total = 3 x 3,500 = 10,500, CONFIRMED")
    void book() throws Exception {
        book(ravi, flat, CHECK_IN, CHECK_OUT, 2)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingTitle").value("Sea view flat"))
                .andExpect(jsonPath("$.guestId").value(ravi.id()))
                .andExpect(jsonPath("$.nights").value(3))
                .andExpect(jsonPath("$.totalPrice").value(10500.00))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("The same nights again (another guest): 409; back to back from the check-out day: 201")
    void noDoubleBooking() throws Exception {
        bookingId(ravi, CHECK_IN, CHECK_OUT);

        book(meera, flat, CHECK_IN.plusDays(2), CHECK_OUT.plusDays(2), 2)       // shares one night
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("listing " + flat + " is already booked for some of these nights"));

        book(meera, flat, CHECK_OUT, CHECK_OUT.plusDays(2), 2).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Booking rules: own listing 409, too many guests 409, deactivated listing 409, unknown listing 404")
    void bookingRules() throws Exception {
        book(asha, flat, CHECK_IN, CHECK_OUT, 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("you cannot book your own listing"));
        book(ravi, flat, CHECK_IN, CHECK_OUT, 5)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("listing " + flat + " takes at most 4 guests"));
        book(ravi, 999_999, CHECK_IN, CHECK_OUT, 2).andExpect(status().isNotFound());

        mvc.perform(delete("/api/listings/" + flat).with(as(asha))).andExpect(status().isNoContent());
        book(ravi, flat, CHECK_IN, CHECK_OUT, 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("listing " + flat + " is not available"));
    }

    @Test
    @DisplayName("Wrong dates are 400: check-in yesterday, check-out before check-in, more than 30 nights")
    void wrongDates() throws Exception {
        book(ravi, flat, LocalDate.now().minusDays(1), CHECK_OUT, 2)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("checkIn must be today or later"));
        book(ravi, flat, CHECK_OUT, CHECK_IN, 2)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("checkOut must be after checkIn"));
        book(ravi, flat, CHECK_IN, CHECK_IN.plusDays(31), 2)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("a booking can be at most 30 nights"));
        book(ravi, flat, CHECK_IN, CHECK_OUT, 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.guests").exists());
    }

    @Test
    @DisplayName("Booked dates disappear from search; the listing is found again for other dates")
    void searchSeesBookings() throws Exception {
        bookingId(ravi, CHECK_IN, CHECK_OUT);

        mvc.perform(get("/api/listings/search?city=" + city + "&checkIn=" + CHECK_IN + "&checkOut=" + CHECK_OUT))
                .andExpect(jsonPath("$.totalListings").value(0));
        mvc.perform(get("/api/listings/search?city=" + city + "&checkIn=" + CHECK_OUT + "&checkOut=" + CHECK_OUT.plusDays(2)))
                .andExpect(jsonPath("$.totalListings").value(1));
    }

    @Test
    @DisplayName("Cancel: the guest's booking becomes CANCELLED, and the nights can be booked again")
    void cancel() throws Exception {
        long booking = bookingId(ravi, CHECK_IN, CHECK_OUT);

        mvc.perform(post("/api/bookings/" + booking + "/cancel").with(as(ravi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        book(meera, flat, CHECK_IN, CHECK_OUT, 2).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Cancel rules: someone else 403, twice 409, on the check-in day 409")
    void cancelRules() throws Exception {
        long booking = bookingId(ravi, CHECK_IN, CHECK_OUT);
        mvc.perform(post("/api/bookings/" + booking + "/cancel").with(as(meera)))
                .andExpect(status().isForbidden());
        // not even the host: the host's cancelling would need its own rules (refund, notice), not built
        mvc.perform(post("/api/bookings/" + booking + "/cancel").with(as(asha)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/bookings/" + booking + "/cancel").with(as(ravi))).andExpect(status().isOk());
        mvc.perform(post("/api/bookings/" + booking + "/cancel").with(as(ravi)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("booking " + booking + " is already cancelled"));

        long today = bookingId(ravi, LocalDate.now(), LocalDate.now().plusDays(1));
        mvc.perform(post("/api/bookings/" + today + "/cancel").with(as(ravi)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("a booking can only be cancelled before its check-in day"));
    }

    @Test
    @DisplayName("Who sees what: the guest their bookings, the host the listing's bookings, anyone else 403")
    void visibility() throws Exception {
        bookingId(ravi, CHECK_IN, CHECK_OUT);

        mvc.perform(get("/api/users/me/bookings").with(as(ravi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/users/me/bookings").with(as(meera)))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/api/listings/" + flat + "/bookings").with(as(asha)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].guestId").value(ravi.id()));
        mvc.perform(get("/api/listings/" + flat + "/bookings").with(as(meera)))
                .andExpect(status().isForbidden());
    }
}
