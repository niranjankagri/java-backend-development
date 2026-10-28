package jbdl.session16;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import jbdl.session16.users.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Authorization: who may do what, once logged in.
 *   401 = "I don't know who you are" (authentication failed)
 *   403 = "I know who you are, and you may not do this" (authorization failed)
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AccountService accounts;

    @Test
    @DisplayName("URL rule: a USER on /api/admin/** gets 403, an ADMIN gets 200")
    void adminUrls() throws Exception {
        mvc.perform(get("/api/admin/users").with(httpBasic("asha", "asha-pass-123")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/admin/users").with(httpBasic("admin", "admin-pass-123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("asha (USER)")));
    }

    @Test
    @DisplayName("Method rule: a user reads their own notes, not someone else's; an ADMIN reads anyone's")
    void ownNotesOnly() throws Exception {
        mvc.perform(post("/api/notes").with(httpBasic("asha", "asha-pass-123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\": \"buy milk\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner").value("asha")); // taken from the login, not from the request

        mvc.perform(get("/api/notes/of/asha").with(httpBasic("asha", "asha-pass-123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("buy milk"));
        mvc.perform(get("/api/notes/of/admin").with(httpBasic("asha", "asha-pass-123")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/notes/of/asha").with(httpBasic("admin", "admin-pass-123")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A disabled account can no longer log in")
    void disabledAccount() throws Exception {
        String username = "temp" + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"" + username + "\", \"password\": \"temporary-pass-1\"}"));
        mvc.perform(get("/api/me").with(httpBasic(username, "temporary-pass-1"))).andExpect(status().isOk());

        mvc.perform(post("/api/admin/users/" + username + "/disable").with(httpBasic("admin", "admin-pass-123")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/me").with(httpBasic(username, "temporary-pass-1"))).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "ravi", roles = "USER")
    @DisplayName("@PreAuthorize protects the SERVICE too, even when called without any HTTP request")
    void methodSecurityWithoutHttp() {
        // @WithMockUser puts a fake logged-in user in the security context (no password check needed)
        assertThatThrownBy(() -> accounts.allUsernames()).isInstanceOf(AuthorizationDeniedException.class);
    }
}
