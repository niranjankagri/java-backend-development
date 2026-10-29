package jbdl.session18;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The login flow and the URL rules, with the whole app running and GitHub NOT contacted.
 *
 * Two kinds of requests appear here:
 * - Real first steps of the OAuth 2 flow (/oauth2/authorization/github and the callback), checked
 *   up to the moment the browser would leave for GitHub.
 * - Requests "already logged in with GitHub": oauth2Login() from spring-security-test puts a
 *   ready-made GitHub login into the request, so the API can be tested without GitHub.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LoginFlowTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Public URL and home page: no login needed")
    void publicPages() throws Exception {
        mvc.perform(get("/api/public/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, anyone can read this"));
        // "/" only forwards to index.html (MockMvc does not follow forwards), so ask for the page itself
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/oauth2/authorization/github")));
    }

    @Test
    @DisplayName("API without login: 401, not a redirect to GitHub")
    void apiWithoutLogin() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Step 1 of the flow: the app sends the browser to GitHub with client id, scope, state and callback")
    void loginStartsAtGitHub() throws Exception {
        mvc.perform(get("/oauth2/authorization/github"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("https://github.com/login/oauth/authorize?")))
                .andExpect(header().string("Location", containsString("response_type=code")))
                .andExpect(header().string("Location", containsString("client_id=set-GITHUB_CLIENT_ID")))
                .andExpect(header().string("Location", containsString("scope=read:user")))
                // state: a random value GitHub sends back unchanged; it proves the answer belongs to OUR request
                .andExpect(header().string("Location", containsString("state=")))
                .andExpect(header().string("Location", containsString("redirect_uri=http://localhost/login/oauth2/code/github")));
    }

    @Test
    @DisplayName("A callback the app never asked for (forged state) is refused: no login")
    void forgedCallbackIsRefused() throws Exception {
        // given: no login was started in this browser session, so there is no saved state to compare with
        // when: someone sends a code to the callback address
        mvc.perform(get("/login/oauth2/code/github").param("code", "stolen-code").param("state", "forged"))
                // then: back to the login page with an error, and nobody is logged in
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("Logged in with GitHub: /api/me shows the GitHub login and the role")
    void meAfterLogin() throws Exception {
        // A user as GitHubMemberService builds it: GitHub's profile, the role, and "login" as the name attribute.
        // (oauth2Login() alone would name the user after the attribute "sub", which GitHub does not send.)
        OAuth2User octocat = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("login", "octocat", "name", "The Octocat", "avatar_url", "https://avatars.githubusercontent.com/u/583231"),
                "login");
        mvc.perform(get("/api/me").with(oauth2Login().oauth2User(octocat)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("octocat"))
                .andExpect(jsonPath("$.name").value("The Octocat"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    @DisplayName("Admin page: 403 for a USER, 200 for an ADMIN")
    void adminPage() throws Exception {
        mvc.perform(get("/api/admin/members").with(oauth2Login().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/members").with(oauth2Login().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }
}
