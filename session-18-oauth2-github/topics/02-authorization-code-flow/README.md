# Topic 2: The authorization code flow, step by step with GitHub

## The whole flow

```
Browser                     This app (client)                    GitHub
   │  1. GET /oauth2/authorization/github
   │ ─────────────────────────▶│
   │  2. 302 to github.com/login/oauth/authorize?client_id&scope&state&redirect_uri&code_challenge
   │ ◀─────────────────────────│   (the app remembers state + PKCE verifier in the session)
   │  3. log in at GitHub, "Authorize app?" → yes
   │ ──────────────────────────────────────────────────────────────▶│
   │  4. 302 to /login/oauth2/code/github?code=ONE-TIME-CODE&state=…
   │ ◀──────────────────────────────────────────────────────────────│
   │  5. GET /login/oauth2/code/github?code&state
   │ ─────────────────────────▶│  6. state matches? then POST /login/oauth/access_token
   │                           │     (code + client secret + PKCE verifier)  ──────────────▶│
   │                           │  7. access token  ◀───────────────────────────────────────│
   │                           │  8. GET api.github.com/user with the token ──────────────▶│
   │                           │     profile: id, login, name, avatar_url  ◀───────────────│
   │  9. logged in (session cookie), redirect to /api/me
   │ ◀─────────────────────────│
```

Steps 1-2 and 5-9 are done by Spring Security; this project only configures them
([`SecurityConfig`](../../src/main/java/jbdl/session18/security/SecurityConfig.java),
[`application.properties`](../../src/main/resources/application.properties)) and adds its own step
after 8 (topic 3).

## Step 2 for real

Run with the placeholder client id and ask for the login start (the app ran on port 8188 here):

```
$ curl -i localhost:8188/oauth2/authorization/github
HTTP/1.1 302
Location: https://github.com/login/oauth/authorize?response_type=code&client_id=set-GITHUB_CLIENT_ID
  &scope=read:user&state=lTn3cTE_7HDnIi4NEgozGxsoL1JITl3Ssw_rYcaxbaw%3D
  &redirect_uri=http://localhost:8188/login/oauth2/code/github
  &code_challenge=fDIJvPY-qpvfwemCoCYFFv5Vyw0Xcx1XdrkrQh0V-yU&code_challenge_method=S256
```

| Parameter | Why |
|---|---|
| `response_type=code` | "Give me an authorization code" (the authorization code flow) |
| `client_id` | Which app is asking (yours, from the GitHub OAuth App) |
| `scope=read:user` | The only permission asked for |
| `state` | A random value; GitHub returns it unchanged. If it doesn't match what this browser session started, the answer is refused (it could be an attacker's code being pushed into your session) |
| `redirect_uri` | Where to come back; GitHub only accepts the callback URL registered for the app |
| `code_challenge`, `S256` | **PKCE**: the hash of a secret only this app knows; at step 6 the app must show the secret itself, so a stolen code is useless to anyone else. Spring Security 7 adds it automatically |

## Why a code, and not the token straight away?

The code travels through the browser (in a URL), where it could leak into history, logs or a
malicious extension. It is therefore worth almost nothing on its own:

- it works **once** and only for a few minutes,
- swapping it for a token needs the **client secret**, which only the server has (step 6),
- and with PKCE also the **code verifier** from this exact login.

The token itself only ever travels from GitHub to the server (step 7), never through the browser.

## Checked in the tests

[`LoginFlowTest`](../../src/test/java/jbdl/session18/LoginFlowTest.java):

- `loginStartsAtGitHub`: step 2, every parameter above.
- `forgedCallbackIsRefused`: a callback with a code but no matching login in this session ends at
  `/login?error`; nobody gets logged in.
- `apiWithoutLogin`: an API call without a login gets `401`, not a redirect (scripts can't log in at GitHub).

Steps 3, 4, 6 and 7 happen at GitHub. They are covered by Spring Security's own tests; here the
login is simulated from step 8 on (topic 3).

## Interview questions

- **Walk me through the authorization code flow.** Redirect to the authorization server with client id, scope, state and redirect URI; user consents; code comes back to the redirect URI; server swaps code + secret for a token; uses the token.
- **What does the state parameter protect against?** CSRF on the callback: an attacker pushing their own authorization code into your session.
- **What is PKCE and why use it even with a client secret?** The client sends a hash of a one-time secret first and the secret itself when swapping the code, so an intercepted code can't be redeemed. Recommended for all clients by OAuth 2.1.
- **Why doesn't the token come back in the redirect?** URLs leak; the code is one-time and needs the secret, the token goes server-to-server.
