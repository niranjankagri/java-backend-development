package jbdl.session18.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * GitHubClient against a FAKE GitHub: MockRestServiceServer answers the RestClient's requests itself,
 * so nothing leaves the machine. Each test says which request it expects and what to answer; the test
 * fails if the client sends something else (wrong address, missing token header ...).
 */
class GitHubClientTest {

    private MockRestServiceServer gitHub;
    private GitHubClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        gitHub = MockRestServiceServer.bindTo(builder).build();
        client = new GitHubClient(builder, "https://api.github.test");
    }

    @Test
    @DisplayName("Sends the user's token and GitHub's headers, and keeps only the fields the app needs")
    void recentRepos() {
        // given: GitHub answers with two repositories (shortened JSON)
        gitHub.expect(requestTo("https://api.github.test/user/repos?sort=updated&per_page=10"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer gho_user_token"))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess("""
                        [
                          {"name": "playwright-framework", "description": "UI tests", "language": "Java",
                           "stargazers_count": 12, "html_url": "https://github.com/octocat/playwright-framework", "fork": false},
                          {"name": "notes", "description": null, "language": null,
                           "stargazers_count": 0, "html_url": "https://github.com/octocat/notes", "fork": false}
                        ]
                        """, MediaType.APPLICATION_JSON));

        // when
        List<RepoSummary> repos = client.recentRepos("gho_user_token");

        // then: GitHub was called exactly as expected, and the answer was mapped
        gitHub.verify();
        assertThat(repos).containsExactly(
                new RepoSummary("playwright-framework", "UI tests", "Java", 12, "https://github.com/octocat/playwright-framework"),
                new RepoSummary("notes", null, null, 0, "https://github.com/octocat/notes"));
    }

    @Test
    @DisplayName("401 from GitHub (token revoked): a clear message to log in again")
    void revokedToken() {
        gitHub.expect(requestTo("https://api.github.test/user/repos?sort=updated&per_page=10")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.recentRepos("gho_revoked"))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("Log in again")
                .extracting("gitHubStatus").isEqualTo(401);
    }

    @Test
    @DisplayName("403 from GitHub: missing scope or rate limit")
    void forbidden() {
        gitHub.expect(requestTo("https://api.github.test/user/repos?sort=updated&per_page=10")).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.recentRepos("gho_limited"))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("rate limit");
    }
}
