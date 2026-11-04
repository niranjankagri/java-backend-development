package jbdl.splitwise;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Session 12 workflow through the HTTP API: register users, create a group, add members, list a
 * user's groups, plus the error cases. Data is committed, so every test uses unique emails.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserAndGroupApiTest {

    @Autowired
    private MockMvc mvc;

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Registers a user with a unique email and returns the new id. */
    private long registerUser(String name) throws Exception {
        String email = name.toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        String body = postJson("/api/users", "{\"name\": \"" + name + "\", \"email\": \"" + email + "\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createGroup(String name, long creatorId) throws Exception {
        String body = postJson("/api/groups", "{\"name\": \"" + name + "\", \"createdByUserId\": " + creatorId + "}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    @DisplayName("Register a user; the email is stored in lower case")
    void registerUser() throws Exception {
        // (an email with spaces around it never reaches the service: @Email validation refuses it first)
        postJson("/api/users", "{\"name\": \"Asha\", \"email\": \"Asha.K@Example.COM\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("asha.k@example.com"));
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
    @DisplayName("Invalid user: 400 with the fields that are wrong")
    void invalidUser() throws Exception {
        postJson("/api/users", "{\"name\": \"\", \"email\": \"not-an-email\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("Group workflow: the creator is the first member, others are added")
    void groupWorkflow() throws Exception {
        long asha = registerUser("Asha");
        long ravi = registerUser("Ravi");
        long group = createGroup("Goa trip", asha);

        postJson("/api/groups/" + group + "/members", "{\"userId\": " + ravi + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(2)))
                .andExpect(jsonPath("$.members[0].name").value("Asha"))
                .andExpect(jsonPath("$.createdBy.id").value(asha));

        mvc.perform(get("/api/users/" + ravi + "/groups"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Goa trip"));
    }

    @Test
    @DisplayName("Adding the same member twice: 409; unknown group or user: 404")
    void memberErrors() throws Exception {
        long asha = registerUser("Asha");
        long group = createGroup("Flat", asha);

        postJson("/api/groups/" + group + "/members", "{\"userId\": " + asha + "}")
                .andExpect(status().isConflict());
        postJson("/api/groups/999999/members", "{\"userId\": " + asha + "}")
                .andExpect(status().isNotFound());
        postJson("/api/groups/" + group + "/members", "{\"userId\": 999999}")
                .andExpect(status().isNotFound());
    }
}
