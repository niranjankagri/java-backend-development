# Topic 3: Login, and who may change what

In Session 21 a request said who it was (`hostId` in the body). Anyone could send any id. Session 22
adds a real login, as Splitwise did in Session 17 (the approach of Session 16).

## How it works

- Registration needs a password (8 to 72 characters); only its **BCrypt hash** is stored
  ([`UserService.register`](../../../airbnb-backend/src/main/java/jbdl/airbnb/users/UserService.java)).
  The answer never contains the password or its hash.
- Every request after that sends **HTTP Basic**: email and password in the `Authorization` header.
  [`EmailUserDetailsService`](../../../airbnb-backend/src/main/java/jbdl/airbnb/security/EmailUserDetailsService.java)
  loads the account by email; Spring Security compares the hash. No session, no cookie (stateless).
- Controllers get the logged-in user as a `Principal`; its name is the email:

```java
@PostMapping("/api/listings")
public ListingView create(@Valid @RequestBody ListingRequest request, Principal principal) {
    return service.create(principal.getName(), request);
}
```

## Open and closed URLs

[`SecurityConfig`](../../../airbnb-backend/src/main/java/jbdl/airbnb/security/SecurityConfig.java):

```java
.requestMatchers(HttpMethod.POST, "/api/users").permitAll()                    // register
.requestMatchers(HttpMethod.GET, "/api/listings/*/bookings").authenticated()   // private, see below
.requestMatchers(HttpMethod.GET, "/api/listings/**").permitAll()               // browsing
.requestMatchers(HttpMethod.GET, "/api/users/*/listings").permitAll()
.anyRequest().authenticated()
```

Browsing is open, as on Airbnb: search, a listing, a host's listings, reviews. Everything that changes
data needs a login.

**The order of the rules matters**: the first rule that matches decides. `GET /api/listings/1/bookings`
also matches `GET /api/listings/**`, so the private rule must come first. In the other order, anyone
could read who booked a listing.

## Two questions, two places

| Question | Answered by | Answer |
|---|---|---|
| Is this a known, logged-in user? | `SecurityConfig` (before the controller) | 401 |
| May THIS user change THIS listing or booking? | the service (it knows the owner) | 403 |

The ownership check of Session 21 stayed where it was, it now compares the logged-in email
([`ListingService.findOwnedBy`](../../../airbnb-backend/src/main/java/jbdl/airbnb/listings/ListingService.java)):

```java
if (!listing.getHost().getEmail().equals(hostEmail)) {
    throw new ForbiddenException("only the host of listing " + id + " can change it");
}
```

What changed in the API since Session 21:

| Session 21 | Session 22 |
|---|---|
| `POST /api/listings` body with `"hostId": 1` | no `hostId`; the host is the login |
| `PUT /api/listings/1` body with `"hostId": 1` | no `hostId` |
| `DELETE /api/listings/1?hostId=1` | `DELETE /api/listings/1` |
| registration `{"name","email"}` | `{"name","email","password"}` |
| | new: `GET /api/users/me` |

## A real run

```
$ curl -i -X POST localhost:8080/api/listings -H 'Content-Type: application/json' -d '{"title":"Sea view flat","city":"Goa","maxGuests":4,"bedrooms":2,"nightlyPrice":3500}'
HTTP/1.1 401
WWW-Authenticate: Basic realm="Realm", charset="UTF-8"

$ curl -u asha@example.com:asha-secret-1 -X POST localhost:8080/api/listings -H 'Content-Type: application/json' -d '{"title":"Sea view flat","city":"Goa","maxGuests":4,"bedrooms":2,"nightlyPrice":3500,"amenities":["WIFI"]}'
{"id":1,"hostId":1,"title":"Sea view flat",...}

$ curl -o /dev/null -w '%{http_code}' localhost:8080/api/listings/1/bookings
401
```

HTTP Basic sends the password with every request, so it must only ever travel over HTTPS (deployment,
Sessions 23 and 24). Session 18 showed the alternative, logging in through GitHub with OAuth 2.

## Tested

[`SecurityRulesTest`](../../../airbnb-backend/src/test/java/jbdl/airbnb/SecurityRulesTest.java): browsing
needs no login; a listing's bookings need one despite the open `GET /api/listings/**`; `/api/users/me`;
short passwords are refused and the hash is never sent back.
[`UserAndListingApiTest`](../../../airbnb-backend/src/test/java/jbdl/airbnb/UserAndListingApiTest.java):
no login or a wrong password is 401, another user's change is 403. The test helper
[`ApiTestSupport`](../../../airbnb-backend/src/test/java/jbdl/airbnb/ApiTestSupport.java) logs in with
`.with(as(user))` (HTTP Basic in MockMvc).

## Interview questions

- **Authentication vs. authorization?** Who are you (401 if unknown) vs. what may you do (403).
- **Why can't the URL rules decide "only the host"?** They only see the URL; who owns listing 7 is in the
  database, so the service decides.
- **Why BCrypt?** Slow on purpose and salted: a stolen table cannot be reversed quickly, and equal
  passwords get different hashes.
- **Why is the order of `requestMatchers` important?** The first match wins; a broad rule placed first
  hides the specific ones after it.
