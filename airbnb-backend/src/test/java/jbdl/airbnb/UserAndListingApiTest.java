package jbdl.airbnb;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

/** Topic 4 through the HTTP API: users, and hosts managing their listings, with the error cases. */
@SpringBootTest
@AutoConfigureMockMvc
class UserAndListingApiTest extends ApiTestSupport {

    @Test
    @DisplayName("Register a user; the email is stored in lower case")
    void registerUser() throws Exception {
        postJson("/api/users", "{\"name\": \"Asha\", \"email\": \"Asha.Host@Example.COM\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("asha.host@example.com"));
    }

    @Test
    @DisplayName("The same email twice: 409 Conflict")
    void duplicateEmail() throws Exception {
        postJson("/api/users", "{\"name\": \"Ravi\", \"email\": \"ravi.dup@example.com\"}").andExpect(status().isCreated());

        postJson("/api/users", "{\"name\": \"Ravi 2\", \"email\": \"RAVI.DUP@example.com\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("email ravi.dup@example.com is already registered"));
    }

    @Test
    @DisplayName("Create a listing: 201 with the host, the price and the amenities")
    void createListing() throws Exception {
        long hostId = registerUser("Asha");

        postJson("/api/listings", listingJson(hostId, "Sea view flat", "Goa", 4, "3500.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hostId").value(hostId))
                .andExpect(jsonPath("$.title").value("Sea view flat"))
                .andExpect(jsonPath("$.nightlyPrice").value(3500.00))
                .andExpect(jsonPath("$.amenities[0]").value("WIFI"))       // sorted in declaration order
                .andExpect(jsonPath("$.amenities[1]").value("KITCHEN"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("Invalid listing: 400 with every wrong field")
    void invalidListing() throws Exception {
        long hostId = registerUser("Asha");

        postJson("/api/listings", """
                {"hostId": %d, "title": "", "city": "Goa", "maxGuests": 0, "bedrooms": 1, "nightlyPrice": 0.5}
                """.formatted(hostId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.maxGuests").exists())
                .andExpect(jsonPath("$.errors.nightlyPrice").exists());
    }

    @Test
    @DisplayName("A listing for a host who does not exist: 404")
    void unknownHost() throws Exception {
        postJson("/api/listings", listingJson(999_999, "Sea view flat", "Goa", 4, "3500.00"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("user 999999 not found"));
    }

    @Test
    @DisplayName("The host can change the listing; anyone else gets 403")
    void onlyHostCanUpdate() throws Exception {
        long hostId = registerUser("Asha");
        long otherId = registerUser("Ravi");
        long listingId = createListing(hostId, "Sea view flat", "Goa", 4, "3500.00");

        mvc.perform(put("/api/listings/" + listingId).contentType(MediaType.APPLICATION_JSON)
                        .content(listingJson(hostId, "Sea view flat, renovated", "Goa", 4, "4000.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Sea view flat, renovated"))
                .andExpect(jsonPath("$.nightlyPrice").value(4000.00));

        mvc.perform(put("/api/listings/" + listingId).contentType(MediaType.APPLICATION_JSON)
                        .content(listingJson(otherId, "Mine now", "Goa", 4, "1.00")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("only the host of listing " + listingId + " can change it"));
    }

    @Test
    @DisplayName("Deactivate: 204, the listing still exists but is no longer active")
    void deactivate() throws Exception {
        long hostId = registerUser("Asha");
        long listingId = createListing(hostId, "Sea view flat", "Goa", 4, "3500.00");

        mvc.perform(delete("/api/listings/" + listingId).param("hostId", String.valueOf(hostId)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/listings/" + listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("A host's listings, oldest first; an unknown user is 404, not an empty list")
    void hostListings() throws Exception {
        long hostId = registerUser("Asha");
        createListing(hostId, "Sea view flat", "Goa", 4, "3500.00");
        createListing(hostId, "Hill cottage", "Nainital", 2, "2500.00");

        mvc.perform(get("/api/users/" + hostId + "/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Sea view flat"))
                .andExpect(jsonPath("$[1].title").value("Hill cottage"));

        mvc.perform(get("/api/users/999999/listings")).andExpect(status().isNotFound());
    }
}
