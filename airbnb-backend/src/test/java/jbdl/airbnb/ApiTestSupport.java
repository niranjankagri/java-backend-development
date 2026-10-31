package jbdl.airbnb;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Shared steps for the API tests: register a user, create a listing, book. The tests of one run share
 * the in-memory database and its data is committed, so every user gets a unique email and every search
 * test its own city.
 *
 * Session 22: changing anything needs a login. A TestUser remembers id, name and email of a registered
 * user; as(user) adds their HTTP Basic login to a request.
 */
abstract class ApiTestSupport {

    static final String PASSWORD = "test-password-123";

    /** A user registered through the API during a test. */
    record TestUser(long id, String name, String email) {
    }

    @Autowired
    protected MockMvc mvc;

    /** The Authorization header for this user: HTTP Basic, checked against the database (Session 16). */
    protected static RequestPostProcessor as(TestUser user) {
        return httpBasic(user.email(), PASSWORD);
    }

    /** POST without a login (registration). */
    protected ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** POST as this user. */
    protected ResultActions postJson(String url, String json, TestUser user) throws Exception {
        return mvc.perform(post(url).with(as(user)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Registers a user with a unique email. */
    protected TestUser registerUser(String name) throws Exception {
        String email = name.toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        String body = postJson("/api/users", """
                {"name": "%s", "email": "%s", "password": "%s"}
                """.formatted(name, email, PASSWORD))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new TestUser(id(body), name, email);
    }

    /** The JSON body of a listing request (Session 22: no hostId any more, the host is the login). */
    protected static String listingJson(String title, String city, int maxGuests, String nightlyPrice) {
        return """
                {"title": "%s", "description": "Near the beach", "city": "%s",
                 "maxGuests": %d, "bedrooms": 2, "nightlyPrice": %s, "amenities": ["WIFI", "KITCHEN"]}
                """.formatted(title, city, maxGuests, nightlyPrice);
    }

    /** Creates a listing as this host and returns the new id. */
    protected long createListing(TestUser host, String title, String city, int maxGuests, String nightlyPrice)
            throws Exception {
        String body = postJson("/api/listings", listingJson(title, city, maxGuests, nightlyPrice), host)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return id(body);
    }

    /** The JSON body of a booking request. */
    protected static String bookingJson(long listingId, LocalDate checkIn, LocalDate checkOut, int guests) {
        return """
                {"listingId": %d, "checkIn": "%s", "checkOut": "%s", "guests": %d}
                """.formatted(listingId, checkIn, checkOut, guests);
    }

    /** A city name no other test uses, e.g. "Goa-3f2a9c". */
    protected static String uniqueCity(String name) {
        return name + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    protected static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
