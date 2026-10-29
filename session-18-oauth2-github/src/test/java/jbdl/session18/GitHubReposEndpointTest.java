package jbdl.session18;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Client;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import jbdl.session18.github.GitHubApiException;
import jbdl.session18.github.GitHubClient;
import jbdl.session18.github.RepoSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * /api/github/repos: the endpoint hands the logged-in user's OWN access token to GitHubClient.
 *
 * oauth2Login() logs a user in, and oauth2Client("github").accessToken(...) gives that login a known GitHub token; GitHubClient is replaced by a
 * Mockito mock (@MockitoBean), so the test can check which token it was given and what happens when
 * GitHub fails, without calling GitHub. GitHubClientTest covers the real HTTP part.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GitHubReposEndpointTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GitHubClient gitHubClient;

    private static final OAuth2AccessToken TOKEN =
            new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "gho_asha", Instant.now(), Instant.now().plusSeconds(3600));

    @Test
    @DisplayName("Uses the logged-in user's access token and returns the repositories")
    void reposWithUsersToken() throws Exception {
        // given: GitHub (the mock) has one repository for this token
        when(gitHubClient.recentRepos("gho_asha"))
                .thenReturn(List.of(new RepoSummary("notes", "My notes", "Java", 3, "https://github.com/asha/notes")));

        // when: Asha, logged in with that token, asks for her repositories
        mvc.perform(get("/api/github/repos").with(oauth2Login()).with(oauth2Client("github").accessToken(TOKEN)))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("notes"))
                .andExpect(jsonPath("$[0].stars").value(3));
        verify(gitHubClient).recentRepos("gho_asha");
    }

    @Test
    @DisplayName("GitHub fails: 502 Bad Gateway with GitHub's status and a readable message")
    void gitHubFails() throws Exception {
        when(gitHubClient.recentRepos("gho_asha")).thenThrow(new GitHubApiException(401, "GitHub no longer accepts this login. Log in again."));

        mvc.perform(get("/api/github/repos").with(oauth2Login()).with(oauth2Client("github").accessToken(TOKEN)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.githubStatus").value(401))
                .andExpect(jsonPath("$.error").value("GitHub no longer accepts this login. Log in again."));
    }

    @Test
    @DisplayName("Not logged in: 401, and GitHub is never called")
    void notLoggedIn() throws Exception {
        mvc.perform(get("/api/github/repos")).andExpect(status().isUnauthorized());
    }
}
