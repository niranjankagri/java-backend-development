# Topic 4: Calling the GitHub API with the user's token

## The authorized client

When the login finishes, Spring Security keeps an **`OAuth2AuthorizedClient`** for the user: which
registration (`github`), whose (the principal name) and, above all, the **access token**. A
controller asks for it by registration id:

```java
@GetMapping("/api/github/repos")
public List<RepoSummary> myRepos(@RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient gitHub) {
    return gitHubClient.recentRepos(gitHub.getAccessToken().getTokenValue());
}
```

([`ApiController`](../../src/main/java/jbdl/session18/api/ApiController.java))

## The client class

[`GitHubClient`](../../src/main/java/jbdl/session18/github/GitHubClient.java) uses `RestClient`
(Session 9) with GitHub's recommended headers, and sends the token on every request:

```
GET https://api.github.com/user/repos?sort=updated&per_page=10
Accept: application/vnd.github+json
X-GitHub-Api-Version: 2022-11-28
Authorization: Bearer gho_...
```

It keeps only five fields of GitHub's large answer
([`RepoSummary`](../../src/main/java/jbdl/session18/github/RepoSummary.java)), and turns GitHub's
errors into one exception with a useful message:

| GitHub says | Meaning | This app answers |
|---|---|---|
| `401` | Token revoked (user removed the app on GitHub) or expired | `502` + "Log in again" |
| `403` | Token lacks a scope, or the rate limit was reached | `502` + that explanation |
| other errors | GitHub trouble | `502` + "try again later" |

`502 Bad Gateway` means "the service behind me failed", which is honest: the request to *this* app
was fine.

## Scopes decide what the token can do

The registration asks for `read:user` only, so `/user/repos` returns **public** repositories. Private
ones need the `repo` scope, which grants full read **and write** access to all repositories:
far more than a "show my repos" page needs. Ask for the smallest scope that does the job; users
see the scope list on the consent screen and refuse greedy apps.

## Rate limits

GitHub allows 5,000 API requests per hour per user token (60 per hour without a token). Its answers
carry `X-RateLimit-Remaining`; a real app would cache results (Session 15) instead of calling GitHub
on every page view.

## Tested without GitHub

- [`GitHubClientTest`](../../src/test/java/jbdl/session18/github/GitHubClientTest.java):
  `MockRestServiceServer` plays GitHub. The test fails unless the client calls the right URL with the
  `Bearer` token and GitHub's headers; it checks the mapping and the 401/403 messages.
- [`GitHubReposEndpointTest`](../../src/test/java/jbdl/session18/GitHubReposEndpointTest.java):
  `oauth2Login()` + `oauth2Client("github").accessToken(...)` log in with a known token, and a
  Mockito mock replaces `GitHubClient`: the endpoint passes **that user's** token, maps GitHub
  errors to 502, and refuses anonymous callers.

## Interview questions

- **How do you get the logged-in user's access token in Spring?** `@RegisteredOAuth2AuthorizedClient("id") OAuth2AuthorizedClient` (or `OAuth2AuthorizedClientManager` outside controllers).
- **What should happen when the provider says 401?** The token was revoked or expired: refresh it if there is a refresh token, otherwise ask the user to log in again.
- **Why ask for minimal scopes?** Least privilege: a leaked token can do less, and users trust the consent screen.
- **How do you test code that calls an external API?** `MockRestServiceServer` (or WireMock) for the HTTP client; mock the client class in controller tests.
