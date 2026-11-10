# Topic 5: OAuth 2 vs OpenID Connect, and the usual mistakes

## OpenID Connect (OIDC)

OAuth 2 says how an app gets a token to *call an API*. It says nothing about *who the user is*: every
provider invented its own "who am I" endpoint (GitHub: `GET /user`). **OpenID Connect** is a thin
layer on OAuth 2 that standardises login:

| | OAuth 2 (GitHub) | OpenID Connect (Google, Microsoft, Okta, Keycloak) |
|---|---|---|
| Ask for | scopes like `read:user` | scope `openid` (+ `profile`, `email`) |
| You get | an access token | an access token **and an ID token** |
| Who is the user? | Call a provider-specific API (`/user`) | Read the ID token: a signed **JWT** with `sub` (user id), `email`, `name`, `iss`, `aud`, `exp` |
| Provider discovery | Configure each URL | `/.well-known/openid-configuration` lists everything |
| Spring | `oauth2Login()` + `OAuth2UserService` | the same `oauth2Login()` + `OidcUserService`, `OidcUser` |

GitHub OAuth Apps speak plain OAuth 2, which is why this session reads the profile from `/user`
and names users by `login`. With Google, Spring Boot would use OIDC automatically (registration id
`google`), and the principal would be an `OidcUser` with the ID token's claims.

## A JWT in one minute

```
eyJhbGciOiJSUzI1NiJ9 . eyJzdWIiOiIxMjMiLCJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJleHAiOjE3...} . Rk9P...
      header (alg)              payload (claims: sub, iss, aud, exp ...), base64, NOT encrypted          signature
```

Anyone can **read** a JWT (it is only base64); nobody can **change** it without breaking the
signature. Check the signature with the issuer's public key, plus `iss`, `aud` and `exp`, before
trusting it. Never put secrets in a JWT.

## The usual mistakes

| Mistake | Why it hurts | Instead |
|---|---|---|
| Client secret in Git or in a browser/mobile app | Anyone can pose as your app | Environment variables on the server; public clients use PKCE without a secret |
| Asking for `repo`, `admin:org` "just in case" | A leaked token can do everything; users refuse | The smallest scopes; ask for more when a feature needs it |
| No `state` check, or a fixed `state` | Login CSRF: an attacker logs you into *their* account | A random state per login (Spring does it) |
| Accepting any `redirect_uri` | Codes sent to an attacker's site | Register exact callback URLs at the provider |
| Using the access token as proof of identity for *your* API | Tokens for another API (audience) can be replayed | OIDC ID token for login; your own session or tokens for your API |
| Storing tokens in `localStorage` in a browser app | Any script on the page (XSS) can steal them | Keep tokens on the server (this session: the session holds them) |
| Logging tokens | Logs are read by many people and kept for years | Never log `Authorization` headers or tokens |
| Ignoring revocation | Users removed your app but you keep calling | Handle 401: drop the token, ask to log in again (topic 4) |

## Where this goes next

- **Session 19 (microservices):** services call each other with the **client credentials** flow, and
  a service acting as an OAuth 2 **resource server** checks JWTs on incoming requests
  (`spring-boot-starter-security-oauth2-resource-server`).
- **Session 21-22 (Airbnb backend):** login with a provider plus the app's own roles, as here.

## Interview questions

- **Difference between OAuth 2 and OpenID Connect?** OAuth 2 delegates access to APIs; OIDC adds a standard identity layer: the `openid` scope, an ID token (JWT) and a user-info endpoint.
- **What is in an ID token?** Claims about the login: `sub`, `iss`, `aud`, `exp`, `iat`, often `email`/`name`; signed by the provider.
- **Is a JWT encrypted?** Normally no, only signed: readable by anyone, tamper-evident.
- **Where should a web app keep tokens?** On the server (session) or in `HttpOnly` cookies, not in `localStorage`.
- **Access token vs ID token?** The access token is for calling an API (its audience is the API); the ID token tells the client who logged in (its audience is the client).
