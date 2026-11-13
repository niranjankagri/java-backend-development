# Session 21: Airbnb Backend System (Part 1)

The start of an Airbnb-like rental backend: hosts offer listings, guests search them for their dates.
The code lives in its own module, [`airbnb-backend`](../airbnb-backend), which grows in Sessions 22
to 24; this folder explains what Session 21 adds.

| # | Topic | README |
|---|---|---|
| 1 | Project overview and requirement analysis | [01-overview-and-requirements](topics/01-overview-and-requirements/README.md) |
| 2 | Architecture and project setup | [02-architecture-and-setup](topics/02-architecture-and-setup/README.md) |
| 3 | Database schema and entity design | [03-schema-and-entity-design](topics/03-schema-and-entity-design/README.md) |
| 4 | Users and listings API | [04-users-and-listings-api](topics/04-users-and-listings-api/README.md) |
| 5 | Search with filters, availability, sorting and pages | [05-search-and-availability](topics/05-search-and-availability/README.md) |

## What works after this session

- All entities and the schema: users, listings with amenities, bookings (stored by the tests now,
  booked through the API in Session 22).
- Users: register (one account per email, stored lower-case), look up.
- Listings: create, read, change and deactivate (only by their host), a host's listings.
- Search: city, guests, budget, free on given dates, three sort orders, pages of up to 50.
- Errors: 400 with every wrong field or parameter, 403 not your listing, 404 unknown ids, 409 email taken.
- 26 tests: [`DateRangeTest`](../airbnb-backend/src/test/java/jbdl/airbnb/DateRangeTest.java),
  [`SchemaDesignTest`](../airbnb-backend/src/test/java/jbdl/airbnb/SchemaDesignTest.java),
  [`UserAndListingApiTest`](../airbnb-backend/src/test/java/jbdl/airbnb/UserAndListingApiTest.java),
  [`ListingSearchTest`](../airbnb-backend/src/test/java/jbdl/airbnb/ListingSearchTest.java).

## What you can do after this session

- Turn an idea into functional and non-functional requirements, and say what is out of scope.
- Choose a modular monolith for a new product and keep its parts apart by package.
- Design entities for a booking domain: half-open date ranges, stored prices, soft delete, indexes,
  a check constraint.
- Write the overlap rule for two stays, in Java and in SQL.
- Build a search API with optional filters, safe sorting and paging.

## Run it

```bash
./mvnw -pl airbnb-backend verify
java -jar airbnb-backend/target/airbnb-backend-1.0.0-SNAPSHOT.jar
curl -X POST localhost:8080/api/users -H 'Content-Type: application/json' -d '{"name":"Asha","email":"asha@example.com"}'
curl -X POST localhost:8080/api/listings -H 'Content-Type: application/json' -d '{"hostId":1,"title":"Sea view flat","city":"Goa","maxGuests":4,"bedrooms":2,"nightlyPrice":3500,"amenities":["WIFI"]}'
curl 'localhost:8080/api/listings/search?city=goa&guests=2&maxPrice=5000&sort=price'
```

More calls and their real answers: [`airbnb-backend/README.md`](../airbnb-backend/README.md).
