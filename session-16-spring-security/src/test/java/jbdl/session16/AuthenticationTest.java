package jbdl.session16;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import jbdl.session16.users.Account;
import jbdl.session16.users.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Authentication against the REAL database accounts: httpBasic("asha", "asha-pass-123") adds the
 * Authorization header, and Spring Security checks it through DatabaseUserDetailsService and BCrypt,
 * exactly as for a real client. (No @WithMockUser here: that would skip the password check.)
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Public URL: no login needed")
    void publicEndpoint() throws Exception {
        mvc.perform(get("/api/public/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, anyone can read this"));
    }

    @Test
    @DisplayName("No credentials: 401 Unauthorized, and the server says how to log in")
    void noCredentials() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Basic realm="))); // + charset="UTF-8" in Spring Security 7
    }

    @Test
    @DisplayName("Wrong password: 401")
    void wrongPassword() throws Exception {
        mvc.perform(get("/api/me").with(httpBasic("asha", "not-her-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Right password: 200, and Spring knows the user and the role")
    void rightPassword() throws Exception {
        mvc.perform(get("/api/me").with(httpBasic("asha", "asha-pass-123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("asha"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_USER"));
    }

    @Test
    @DisplayName("Register, then log in with the new account; the password is stored only as a BCrypt hash")
    void registerAndLogin() throws Exception {
        String username = "user" + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"" + username + "\", \"password\": \"a-long-password-1\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/me").with(httpBasic(username, "a-long-password-1")))
                .andExpect(status().isOk());

        Account stored = accounts.findByUsername(username).orElseThrow();
        assertThat(stored.getPasswordHash()).startsWith("$2a$").doesNotContain("a-long-password-1");
        assertThat(passwordEncoder.matches("a-long-password-1", stored.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Registration rules: unique username (409), password at least 12 characters (400)")
    void registrationRules() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"asha\", \"password\": \"another-password\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"shorty\", \"password\": \"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Same password, two users: two different hashes (BCrypt adds a random salt)")
    void saltedHashes() {
        String first = passwordEncoder.encode("same-password-123");
        String second = passwordEncoder.encode("same-password-123");

        assertThat(first).isNotEqualTo(second);
        assertThat(passwordEncoder.matches("same-password-123", first)).isTrue();
        assertThat(passwordEncoder.matches("same-password-123", second)).isTrue();
    }
}
