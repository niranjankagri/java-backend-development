package jbdl.session18.github;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls the GitHub REST API on behalf of the logged-in user.
 *
 * The access token is what OAuth 2 is for: GitHub gave it to THIS app for THIS user, limited to the
 * scopes the user agreed to (here read:user). The app never sees the user's GitHub password. Every
 * request carries the token in the Authorization header:
 *   Authorization: Bearer gho_xxxxxxxx
 */
@Component
public class GitHubClient {

    private final RestClient http;

    public GitHubClient(RestClient.Builder builder, @Value("${github.api-url:https://api.github.com}") String apiUrl) {
        this.http = builder
                .baseUrl(apiUrl)
                // GitHub asks every client to say which response format and API version it expects
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    /** The user's 10 most recently updated repositories that this token may see. */
    public List<RepoSummary> recentRepos(String accessToken) {
        // GitHub answers with a JSON array of objects; each object becomes a Map here.
        List<Map<String, Object>> repos = http.get()
                .uri("/user/repos?sort=updated&per_page=10")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                // Turn GitHub's error answers into one exception with a helpful message.
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw toException(response.getStatusCode().value());
                })
                // ParameterizedTypeReference keeps the generic type List<Map<String, Object>> at run time
                // (Java erases generics). The simpler alternative is .body(Map[].class) and a cast per item.
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        List<RepoSummary> result = new ArrayList<>();
        if (repos == null) {
            return result;
        }
        for (Map<String, Object> repo : repos) {
            Number stars = (Number) repo.get("stargazers_count");
            result.add(new RepoSummary(
                    (String) repo.get("name"),
                    (String) repo.get("description"),
                    (String) repo.get("language"),
                    stars == null ? 0 : stars.intValue(),
                    (String) repo.get("html_url")));
        }
        return result;
    }

    private static GitHubApiException toException(int status) {
        if (status == 401) {
            return new GitHubApiException(status, "GitHub no longer accepts this login (the token was revoked or expired). Log in again.");
        }
        if (status == 403) {
            return new GitHubApiException(status, "GitHub refused the request: the token lacks a scope, or the rate limit was reached.");
        }
        return new GitHubApiException(status, "GitHub answered with an error (" + status + "). Try again later.");
    }
}
