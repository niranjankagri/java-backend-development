# Topic 3: Login with GitHub in Spring Boot

## What Spring Boot needs

1. The starter `spring-boot-starter-security-oauth2-client` ([`pom.xml`](../../pom.xml)).
2. A client **registration**, in [`application.properties`](../../src/main/resources/application.properties):

```properties
spring.security.oauth2.client.registration.github.client-id=${GITHUB_CLIENT_ID:set-GITHUB_CLIENT_ID}
spring.security.oauth2.client.registration.github.client-secret=${GITHUB_CLIENT_SECRET:set-GITHUB_CLIENT_SECRET}
spring.security.oauth2.client.registration.github.scope=read:user
```

The registration id `github` is special: Spring knows GitHub's addresses (authorize, token, user
info) and fills them in. For another provider you would add `...provider.<id>.authorization-uri`,
`token-uri`, `user-info-uri` yourself. The id and secret come from **environment variables**: a
secret in Git is a leaked secret.

3. `oauth2Login()` in the security rules
   ([`SecurityConfig`](../../src/main/java/jbdl/session18/security/SecurityConfig.java)).

That alone is a working "Log in with GitHub". The rest of this topic is about making the GitHub user
a user of **this** app.

## From GitHub profile to member

After the token exchange Spring Security calls an `OAuth2UserService` with the token. The default
one calls `GET https://api.github.com/user` and returns the profile as attributes:

```json
{ "id": 583231, "login": "octocat", "name": "The Octocat", "avatar_url": "https://avatars...", ... }
```

[`GitHubMemberService`](../../src/main/java/jbdl/session18/security/GitHubMemberService.java)
wraps the default and adds two things:

| Step | Why |
|---|---|
| `MemberService.recordLogin(id, login, name, avatar)` | The app needs its own record: role, first/last login, login count ([`Member`](../../src/main/java/jbdl/session18/members/Member.java)) |
| Add `ROLE_USER` or `ROLE_ADMIN` to the authorities | So `hasRole("ADMIN")` works exactly like in Session 16 |
| Name attribute `login` | `principal.getName()` returns `octocat`, not a number |

Design choices in [`MemberService`](../../src/main/java/jbdl/session18/members/MemberService.java):

- The key is GitHub's **numeric id**, not the login: people rename their GitHub accounts
  (test `renamedAccount`).
- There is no registration form: the first GitHub login creates the member.
- Admins are GitHub logins listed in `APP_ADMIN_LOGINS`; everybody else is a `USER`.
- No password column: GitHub checks passwords, not this app.

## Session cookie, not stateless

Session 16's API was **stateless**: every request carried a username and password. Here the login
happens once, at GitHub, and is remembered in a normal **server session** (`JSESSIONID` cookie),
like any website. Two consequences, both in `SecurityConfig`:

- **CSRF protection stays on**: the browser sends the cookie automatically, so a malicious page
  could otherwise make state-changing requests in your name. Logging out is a `POST` with the token.
- **API requests without a login get `401`** (`defaultAuthenticationEntryPointFor`), while the
  login itself starts from the home page link `/oauth2/authorization/github`.

## Run it with your own GitHub OAuth App

1. GitHub → Settings → Developer settings → **OAuth Apps** → *New OAuth App*.
2. Homepage URL `http://localhost:8080`, Authorization callback URL
   `http://localhost:8080/login/oauth2/code/github`.
3. Copy the **Client ID**, *Generate a new client secret*, copy it (shown only once).
4. Run:

```bash
./mvnw -pl session-18-oauth2-github package
GITHUB_CLIENT_ID=... GITHUB_CLIENT_SECRET=... APP_ADMIN_LOGINS=<your-github-login> \
  java -jar session-18-oauth2-github/target/session-18-oauth2-github-1.0.0-SNAPSHOT.jar
```

5. Open <http://localhost:8080>, click **Log in with GitHub**, authorize, and you land on `/api/me`.

## Tested without GitHub

- [`GitHubMemberServiceTest`](../../src/test/java/jbdl/session18/security/GitHubMemberServiceTest.java):
  a fake profile source stands in for GitHub's `/user` call; the real service, member logic and
  database run. First login, renamed account, admin role.
- [`LoginFlowTest`](../../src/test/java/jbdl/session18/LoginFlowTest.java): `oauth2Login().oauth2User(...)`
  from spring-security-test puts a ready-made GitHub login into a request: `/api/me`, admin page 403/200.

## Interview questions

- **What do you need to add "Log in with Google/GitHub" to Spring Boot?** The OAuth 2 client starter, a client registration (id, secret, scopes) and `oauth2Login()`.
- **How do you give OAuth 2 users your own roles?** A custom `OAuth2UserService` (OIDC: `OidcUserService`) that loads or creates the local user and adds authorities.
- **Why store the provider's user id rather than the username or e-mail?** Usernames and e-mails can change or be reused; the id is stable.
- **How do you test OAuth 2 login without the provider?** spring-security-test's `oauth2Login()` / `oidcLogin()` request post-processors, and a fake user service.
