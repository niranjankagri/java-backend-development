# Session 18: OAuth 2 & GitHub Integration

"Log in with GitHub" instead of passwords: the OAuth 2 authorization code flow, local member records
with roles for GitHub users, and calls to the GitHub API with the user's own access token.

| # | Topic | Code | README |
|---|---|---|---|
| 1 | OAuth 2 concepts: roles, scopes, tokens, flows | [`security/SecurityConfig`](src/main/java/jbdl/session18/security/SecurityConfig.java) | [01-oauth2-concepts](topics/01-oauth2-concepts/README.md) |
| 2 | The authorization code flow, step by step with GitHub | [`LoginFlowTest`](src/test/java/jbdl/session18/LoginFlowTest.java) | [02-authorization-code-flow](topics/02-authorization-code-flow/README.md) |
| 3 | Login with GitHub in Spring Boot | [`security/GitHubMemberService`](src/main/java/jbdl/session18/security/GitHubMemberService.java), [`members`](src/main/java/jbdl/session18/members) | [03-login-with-github](topics/03-login-with-github/README.md) |
| 4 | Calling the GitHub API with the user's token | [`github/GitHubClient`](src/main/java/jbdl/session18/github/GitHubClient.java), [`api/ApiController`](src/main/java/jbdl/session18/api/ApiController.java) | [04-calling-the-github-api](topics/04-calling-the-github-api/README.md) |
| 5 | OAuth 2 vs OpenID Connect, and the usual mistakes | (README only) | [05-oauth2-vs-openid-connect-and-pitfalls](topics/05-oauth2-vs-openid-connect-and-pitfalls/README.md) |

## What you can do after this session

- Explain the four OAuth 2 roles, scopes, codes and tokens, and which flows to use (and not use).
- Walk through the authorization code flow, including `state` and PKCE, and say why the code is one-time.
- Add "Log in with GitHub" to Spring Boot and map provider users to local users with roles.
- Call a provider's API with the user's access token, handle revoked tokens, and keep scopes minimal.
- Tell OAuth 2 from OpenID Connect and avoid the common token and secret mistakes.
- Test all of it without the provider: `oauth2Login()`, `oauth2Client()`, `MockRestServiceServer`.

## Run it

```bash
./mvnw -pl session-18-oauth2-github verify          # 15 tests, none of them contacts GitHub
```

Without a GitHub OAuth App the app still starts (placeholder client id), so the first steps can be
seen; port 8188 was used here:

```
$ curl localhost:8188/api/public/hello
Hello, anyone can read this
$ curl -i localhost:8188/api/me
HTTP/1.1 401
$ curl -i localhost:8188/oauth2/authorization/github
HTTP/1.1 302
Location: https://github.com/login/oauth/authorize?response_type=code&client_id=set-GITHUB_CLIENT_ID&scope=read:user&state=...&redirect_uri=http://localhost:8188/login/oauth2/code/github&code_challenge=...&code_challenge_method=S256
```

For a real login, register a GitHub OAuth App (callback `http://localhost:8080/login/oauth2/code/github`)
and start the app with its id and secret, as described in
[topic 3](topics/03-login-with-github/README.md#run-it-with-your-own-github-oauth-app):

```bash
GITHUB_CLIENT_ID=... GITHUB_CLIENT_SECRET=... APP_ADMIN_LOGINS=<your-github-login> \
  java -jar session-18-oauth2-github/target/session-18-oauth2-github-1.0.0-SNAPSHOT.jar
```

Then open <http://localhost:8080> and click **Log in with GitHub**.
