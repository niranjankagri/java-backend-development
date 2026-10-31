package jbdl.airbnb;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

/** Session 22, topic 3: which URLs are open, which need a login (SecurityConfig). */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesTest extends ApiTestSupport {

    @Test
    @DisplayName("Browsing is open: search, a listing, a host's listings, reviews; no login needed")
    void browsingIsOpen() throws Exception {
        TestUser host = registerUser("Asha");
        long listing = createListing(host, "Sea view flat", uniqueCity("Goa"), 4, "3500.00");

        mvc.perform(get("/api/listings/search?city=Goa")).andExpect(status().isOk());
        mvc.perform(get("/api/listings/" + listing)).andExpect(status().isOk());
        mvc.perform(get("/api/users/" + host.id() + "/listings")).andExpect(status().isOk());
        mvc.perform(get("/api/listings/" + listing + "/reviews")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("A listing's bookings need a login, although GET /api/listings/** is open (rule order)")
    void listingBookingsNeedLogin() throws Exception {
        TestUser host = registerUser("Asha");
        long listing = createListing(host, "Sea view flat", uniqueCity("Goa"), 4, "3500.00");

        mvc.perform(get("/api/listings/" + listing + "/bookings")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Who am I: 401 without a login; the logged-in user with one")
    void me() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());

        TestUser ravi = registerUser("Ravi");
        mvc.perform(get("/api/users/me").with(as(ravi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ravi.id()))
                .andExpect(jsonPath("$.email").value(ravi.email()));
    }

    @Test
    @DisplayName("Registration needs a password of at least 8 characters; it is never sent back")
    void password() throws Exception {
        postJson("/api/users", "{\"name\": \"Ravi\", \"email\": \"ravi.short@example.com\", \"password\": \"short\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        postJson("/api/users", "{\"name\": \"Ravi\", \"email\": \"ravi.ok@example.com\", \"password\": \"" + PASSWORD + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
}
