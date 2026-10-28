package jbdl.splitwise;

import static jbdl.splitwise.ApiTestSupport.as;
import static jbdl.splitwise.ApiTestSupport.createGroup;
import static jbdl.splitwise.ApiTestSupport.postJson;
import static jbdl.splitwise.ApiTestSupport.register;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jbdl.splitwise.ApiTestSupport.TestUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Session 12 workflow through the HTTP API (updated in Session 17 for login): register users, create a
 * group, add members, list a user's groups, plus the error cases. Data is committed, so every test
 * uses new users with unique emails.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserAndGroupApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Register a user; the email is stored in lower case")
    void registerUser() throws Exception {
        // (an email with spaces around it never reaches the service: @Email validation refuses it first)
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Asha\", \"email\": \"Asha.K@Example.COM\", \"password\": \"a-good-password-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("asha.k@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("The same email twice: 409 Conflict")
    void duplicateEmail() throws Exception {
        String body = "{\"name\": \"Ravi\", \"email\": \"ravi.dup@example.com\", \"password\": \"a-good-password-1\"}";
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());

        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body.replace("ravi.dup", "RAVI.DUP")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("email ravi.dup@example.com is already registered"));
    }

    @Test
    @DisplayName("Invalid user: 400 with the fields that are wrong (incl. a too-short password)")
    void invalidUser() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"email\": \"not-an-email\", \"password\": \"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("Group workflow: the logged-in creator is the first member, others are added")
    void groupWorkflow() throws Exception {
        TestUser asha = register(mvc, "Asha");
        TestUser ravi = register(mvc, "Ravi");
        long group = createGroup(mvc, "Goa trip", asha);

        postJson(mvc, "/api/groups/" + group + "/members", "{\"userId\": " + ravi.id() + "}", asha)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(2)))
                .andExpect(jsonPath("$.members[0].name").value("Asha"))
                .andExpect(jsonPath("$.createdBy.id").value(asha.id()));

        mvc.perform(get("/api/users/" + ravi.id() + "/groups").with(as(ravi)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Goa trip"));
    }

    @Test
    @DisplayName("Adding the same member twice: 409; unknown user: 404")
    void memberErrors() throws Exception {
        TestUser asha = register(mvc, "Asha");
        long group = createGroup(mvc, "Flat", asha);

        postJson(mvc, "/api/groups/" + group + "/members", "{\"userId\": " + asha.id() + "}", asha)
                .andExpect(status().isConflict());
        postJson(mvc, "/api/groups/" + group + "/members", "{\"userId\": 999999}", asha)
                .andExpect(status().isNotFound());
    }
}
