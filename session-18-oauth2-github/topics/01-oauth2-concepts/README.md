# Topic 1: OAuth 2 concepts

## The problem OAuth 2 solves

An app wants to read something of yours on another service: your GitHub profile, your Google
calendar. The bad old way was to give the app your GitHub **password**. Then the app could do
*everything* you can, for ever, and the only way to stop it was to change your password.

OAuth 2 gives the app a **token** instead: a key that GitHub issued for **this app**, for **this
user**, for **these permissions only** (scopes), that can **expire** and that the user can **revoke**
on GitHub at any time. The app never sees the password.

## The four roles

| Role | In this session | What it does |
|---|---|---|
| **Resource owner** | You, the person logging in | Owns the data and says yes or no |
| **Client** | This Spring Boot app | Wants access; registered at GitHub with a client id and secret |
| **Authorization server** | `github.com/login/oauth/...` | Checks who you are, asks for consent, issues tokens |
| **Resource server** | `api.github.com` | Holds the data; accepts requests that carry a valid token |

At GitHub the authorization server and the resource server are run by the same company, but they
are different addresses with different jobs.

## Words you will meet

| Word | Meaning |
|---|---|
| **Client id** | The app's public name at GitHub (appears in URLs) |
| **Client secret** | The app's password at GitHub; only the server knows it, never the browser or Git |
| **Scope** | One permission, e.g. `read:user` (read the profile) or `repo` (full access to repositories) |
| **Consent screen** | GitHub's page "*App X wants to read your profile. Authorize?*" |
| **Authorization code** | A one-time, short-lived code GitHub gives the browser after consent |
| **Access token** | What the app sends to the API: `Authorization: Bearer gho_...` |
| **Refresh token** | Gets a new access token without asking the user again (GitHub OAuth Apps don't issue one; their tokens don't expire until revoked) |
| **Redirect URI (callback)** | Where GitHub sends the browser back: `http://localhost:8080/login/oauth2/code/github` |

## Authorization, not authentication

OAuth 2 was designed for **authorization**: "may this app read my repositories?". Logging in
("who is this?") is a use people built on top: if GitHub gives the app a token for *your* account and
the app can read *your* profile with it, the app knows it is you. Topic 5 shows how OpenID Connect
turned that into a proper standard.

In this session's [`SecurityConfig`](../../src/main/java/jbdl/session18/security/SecurityConfig.java),
`oauth2Login()` does the logging-in use, and the token is used again in topic 4 for the authorization use.

## The grant types (flows)

| Flow | For | Status |
|---|---|---|
| **Authorization code** (+ PKCE) | Apps with a user in a browser: web apps, mobile apps, single-page apps | The one to use; topic 2 |
| **Client credentials** | Service to service, no user (Session 19: microservices) | Use it |
| **Device code** | TVs, command-line tools (`gh auth login`) | Use it |
| Implicit | Old browser apps: the token came back in the URL | Deprecated: tokens leaked through history and logs |
| Password | The app collects your password and trades it for a token | Deprecated: the whole point was not to share passwords |

## Interview questions

- **What problem does OAuth 2 solve?** Giving an app limited, revocable access to your data on another service without sharing your password.
- **Name the four roles.** Resource owner, client, authorization server, resource server.
- **What is a scope?** One permission the app asks for; the token can only do what its scopes allow.
- **Is OAuth 2 an authentication protocol?** No, it is for authorization; login is built on top of it, properly with OpenID Connect.
- **Why are the implicit and password grants deprecated?** Implicit exposed tokens in URLs; password makes users give their password to the app.
