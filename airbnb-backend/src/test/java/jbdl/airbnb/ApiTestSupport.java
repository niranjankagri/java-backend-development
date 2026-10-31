package jbdl.airbnb;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Shared steps for the API tests: register a user, create a listing. The tests of one run share the
 * in-memory database and its data is committed, so every user gets a unique email and every search
 * test its own city.
 */
abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mvc;

    protected ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Registers a user with a unique email and returns the new id. */
    protected long registerUser(String name) throws Exception {
        String email = name.toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        String body = postJson("/api/users", "{\"name\": \"" + name + "\", \"email\": \"" + email + "\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return id(body);
    }

    /** The JSON body of a listing request. */
    protected static String listingJson(long hostId, String title, String city, int maxGuests, String nightlyPrice) {
        return """
                {"hostId": %d, "title": "%s", "description": "Near the beach", "city": "%s",
                 "maxGuests": %d, "bedrooms": 2, "nightlyPrice": %s, "amenities": ["WIFI", "KITCHEN"]}
                """.formatted(hostId, title, city, maxGuests, nightlyPrice);
    }

    /** Creates a listing and returns the new id. */
    protected long createListing(long hostId, String title, String city, int maxGuests, String nightlyPrice)
            throws Exception {
        String body = postJson("/api/listings", listingJson(hostId, title, city, maxGuests, nightlyPrice))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return id(body);
    }

    /** A city name no other test uses, e.g. "Goa-3f2a9c". */
    protected static String uniqueCity(String name) {
        return name + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    private static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
