package jbdl.airbnb;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import jbdl.airbnb.common.TimeConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * "Today" is counted in the business's zone (TimeConfig), not the server's. The clock is frozen at
 * 24 Nov 2026, 20:00 UTC: on a UTC server that is still the 24th, in India it is already 25 Nov, 01:30.
 * @TestBean replaces the application's Clock bean with the one from the static method clock() below.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BusinessDayTest extends ApiTestSupport {

    private static final LocalDate UTC_DATE = LocalDate.of(2026, 11, 24);
    private static final LocalDate INDIA_DATE = LocalDate.of(2026, 11, 25);

    @TestBean
    private Clock clock;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2026-11-24T20:00:00Z"), TimeConfig.BUSINESS_ZONE);
    }

    @Test
    @DisplayName("Booking: the 24th is already yesterday in India (400); the 25th is today (201)")
    void bookingCountsDaysInIndia() throws Exception {
        TestUser host = registerUser("Asha");
        TestUser guest = registerUser("Ravi");
        long flat = createListing(host, "Sea view flat", uniqueCity("Goa"), 4, "3500.00");

        postJson("/api/bookings", bookingJson(flat, UTC_DATE, UTC_DATE.plusDays(2), 2), guest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("checkIn must be today or later"));
        postJson("/api/bookings", bookingJson(flat, INDIA_DATE, INDIA_DATE.plusDays(2), 2), guest)
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Search with dates: a check-in on the 24th is refused, the 25th is fine")
    void searchCountsDaysInIndia() throws Exception {
        String city = uniqueCity("Goa");

        mvc.perform(get("/api/listings/search").param("city", city)
                        .param("checkIn", UTC_DATE.toString()).param("checkOut", INDIA_DATE.toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/listings/search").param("city", city)
                        .param("checkIn", INDIA_DATE.toString()).param("checkOut", INDIA_DATE.plusDays(1).toString()))
                .andExpect(status().isOk());
    }
}
