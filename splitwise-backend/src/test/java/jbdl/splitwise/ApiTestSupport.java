package jbdl.splitwise;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Shared helpers for the API tests (Session 17: every call now needs a login). A TestUser remembers
 * the id and email of a registered user; as(user) adds their HTTP Basic credentials to a request.
 */
final class ApiTestSupport {

    static final String PASSWORD = "test-password-123";

    /** A user registered through the API during a test. */
    record TestUser(long id, String name, String email) {
    }

    private ApiTestSupport() {
    }

    /** The Authorization header for this user (Session 16: HTTP Basic, checked against the database). */
    static RequestPostProcessor as(TestUser user) {
        return httpBasic(user.email(), PASSWORD);
    }

    static ResultActions postJson(MockMvc mvc, String url, String json, TestUser user) throws Exception {
        return mvc.perform(post(url).with(as(user)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Registers a new user with a unique email (registration needs no login). */
    static TestUser register(MockMvc mvc, String name) throws Exception {
        String email = name.toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        String body = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\", \"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new TestUser(idOf(body), name, email);
    }

    /** Creates a group as this user (the creator now comes from the login, not from the body). */
    static long createGroup(MockMvc mvc, String name, TestUser creator) throws Exception {
        String body = postJson(mvc, "/api/groups", "{\"name\": \"" + name + "\"}", creator)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(body);
    }

    static long idOf(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
