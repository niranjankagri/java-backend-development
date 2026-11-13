# Topic 4: Users and listings API

| Method | URL | What | Answers |
|---|---|---|---|
| POST | `/api/users` | register | 201, 400, 409 email taken |
| GET | `/api/users/{id}` | a user | 200, 404 |
| POST | `/api/listings` | create a listing (body with `hostId`) | 201, 400, 404 unknown host |
| GET | `/api/listings/{id}` | a listing, also an inactive one | 200, 404 |
| PUT | `/api/listings/{id}` | change it; only its host | 200, 400, 403, 404 |
| DELETE | `/api/listings/{id}?hostId=` | deactivate; only its host | 204, 403, 404 |
| GET | `/api/users/{id}/listings` | a host's listings, oldest first | 200, 404 |

## Validation in the request record

[`ListingDtos.ListingRequest`](../../../airbnb-backend/src/main/java/jbdl/airbnb/listings/ListingDtos.java):

```java
public record ListingRequest(
        @NotNull Long hostId,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 80) String city,
        @Min(1) @Max(16) int maxGuests,
        @Min(0) @Max(20) int bedrooms,
        @NotNull @DecimalMin("1.00") @Digits(integer = 8, fraction = 2) BigDecimal nightlyPrice,
        Set<Amenity> amenities) {
}
```

`@Valid` on the controller parameter checks it before the method runs; every broken rule comes back
at once (real run):

```
$ curl -X POST localhost:8080/api/listings -H 'Content-Type: application/json' -d '{"hostId":1,"title":"","city":"Goa","maxGuests":0,"bedrooms":1,"nightlyPrice":0.5}'
{"detail":"request body is invalid","instance":"/api/listings","status":400,"title":"Bad Request",
 "errors":{"maxGuests":"must be greater than or equal to 1","nightlyPrice":"must be greater than or equal to 1.00","title":"must not be blank"}}
```

`@Digits(integer = 8, fraction = 2)` matches the column `numeric(10,2)`: a price the database could not
store is refused as 400, not as a database error (500).

## Only the host may change a listing

[`ListingService`](../../../airbnb-backend/src/main/java/jbdl/airbnb/listings/ListingService.java):

```java
private Listing findOwnedBy(long id, long hostId) {
    Listing listing = find(id);                                   // 404 if it does not exist
    if (listing.getHost().getId() != hostId) {
        throw new ForbiddenException("only the host of listing " + id + " can change it");   // 403
    }
    return listing;
}
```

```
$ curl -X PUT localhost:8080/api/listings/1 -H 'Content-Type: application/json' -d '{"hostId":2,"title":"Mine now","city":"Goa","maxGuests":4,"bedrooms":2,"nightlyPrice":1}'
{"detail":"only the host of listing 1 can change it","instance":"/api/listings/1","status":403,"title":"Forbidden"}
```

Until Session 22 the caller states `hostId` themselves, so this protects against mistakes, not
attackers; with login, `hostId` becomes the logged-in user and this method stays the same.

`update` needs no `save()`: the listing was loaded inside the transaction, so JPA notices the changes
and writes them when the transaction ends (Session 11).

## A real run

```
$ curl -X POST localhost:8080/api/users -H 'Content-Type: application/json' -d '{"name":"Asha","email":"asha@example.com"}'
{"id":1,"name":"Asha","email":"asha@example.com"}

$ curl -X POST localhost:8080/api/listings -H 'Content-Type: application/json' -d '{"hostId":1,"title":"Sea view flat","description":"Two minutes to the beach","city":"Goa","maxGuests":4,"bedrooms":2,"nightlyPrice":3500,"amenities":["WIFI","KITCHEN","AIR_CONDITIONING"]}'
{"id":1,"hostId":1,"title":"Sea view flat","description":"Two minutes to the beach","city":"Goa","maxGuests":4,"bedrooms":2,
 "nightlyPrice":3500,"amenities":["WIFI","KITCHEN","AIR_CONDITIONING"],"active":true,"createdAt":"2026-10-31T06:11:22.389076800Z"}
```

## Tested

[`UserAndListingApiTest`](../../../airbnb-backend/src/test/java/jbdl/airbnb/UserAndListingApiTest.java)
through MockMvc: registration (lower-case email, 409 for a repeat), a listing created with its
amenities, 400 with every wrong field, 404 for an unknown host, the host's update works and anyone
else's is 403, deactivation (204, still readable, `active: false`), a host's listings in order and 404
for an unknown user. Shared steps (register a user with a unique email, create a listing) are in
[`ApiTestSupport`](../../../airbnb-backend/src/test/java/jbdl/airbnb/ApiTestSupport.java).

## Interview questions

- **401 vs. 403?** 401: we do not know who you are (log in). 403: we know, and you may not do this.
- **PUT vs. PATCH?** PUT replaces the whole resource with the body (every field sent); PATCH changes only
  the fields sent.
- **Why does DELETE deactivate instead of deleting?** History and foreign keys (topic 3). The method
  says what the client wants (remove it from the market); how it is stored is the server's business.
