# Topic 5: Search with filters, availability, sorting and pages

```
GET /api/listings/search?city=Goa&guests=2&checkIn=2026-12-20&checkOut=2026-12-23&maxPrice=5000&sort=price&page=0&size=10
```

| Parameter | Default | Rule |
|---|---|---|
| `city` | (required) | any upper/lower case |
| `guests` | 1 | 1 to 16; the listing must take at least that many |
| `maxPrice` | no limit | nightly price up to this |
| `checkIn`, `checkOut` | none | both or neither; ISO dates; check-in today or later, check-out after it |
| `sort` | `newest` | `newest`, `price` (cheapest first) or `price_desc` |
| `page`, `size` | 0, 10 | page from 0; size 1 to 50 |

## The queries

[`ListingRepository`](../../../airbnb-backend/src/main/java/jbdl/airbnb/listings/ListingRepository.java),
in JPQL (Session 11). With dates:

```sql
select l from Listing l
where l.active = true
  and lower(l.city) = lower(:city)
  and l.maxGuests >= :guests
  and l.nightlyPrice <= :maxPrice
  and not exists (
      select b from Booking b
      where b.listing = l
        and b.status = jbdl.airbnb.bookings.BookingStatus.CONFIRMED
        and b.checkIn < :checkOut
        and b.checkOut > :checkIn)
```

The last two lines are the overlap rule of topic 3 (`DateRange.overlaps`), in SQL: a listing is free
when **no** confirmed booking overlaps the stay. Cancelled bookings do not count. Without dates, a
second query leaves out the `not exists` part.

`maxPrice` has no "missing" case in the query: the service passes the highest price the column can hold
when the client gave none (`NO_PRICE_LIMIT`), which is simpler than two more queries.

## Pages and sorting

The repository methods take a `Pageable` and return a `Page`: Spring Data adds `LIMIT/OFFSET` and
`ORDER BY`, and runs a second query that counts all matches. The answer
([`SearchPage`](../../../airbnb-backend/src/main/java/jbdl/airbnb/listings/ListingDtos.java)) has what a
client needs for "page 2 of 3": `listings`, `page`, `size`, `totalListings`, `totalPages`.

Only three sort orders exist
([`ListingService.sortOrder`](../../../airbnb-backend/src/main/java/jbdl/airbnb/listings/ListingService.java)):

```java
case "price":
    return Sort.by(Sort.Order.asc("nightlyPrice"), Sort.Order.asc("id"));
```

- The client's text never goes straight into `Sort.by(...)`: otherwise anyone could sort by any field.
- The `id` at the end makes the order complete. Two listings with the same price would otherwise come
  back in any order, and one could appear on two pages or on none.
- `size` is at most 50, so nobody can ask for a million rows at once.

## A real run

Three listings in Goa (3,500 for 4 guests, 1,800 for 2, 12,000 for 10):

```
$ curl 'localhost:8080/api/listings/search?city=goa&guests=2&maxPrice=5000&sort=price'
{"listings":[{"id":2,"hostId":1,"title":"Beach hut","description":null,"city":"Goa","maxGuests":2,"bedrooms":1,"nightlyPrice":1800.00,
              "amenities":["WIFI"],"active":true,"createdAt":"2026-10-31T06:11:22.503080Z"},
             {"id":1,"hostId":1,"title":"Sea view flat","description":"Two minutes to the beach","city":"Goa","maxGuests":4,"bedrooms":2,"nightlyPrice":3500.00,
              "amenities":["WIFI","KITCHEN","AIR_CONDITIONING"],"active":true,"createdAt":"2026-10-31T06:11:22.389077Z"}],
 "page":0,"size":10,"totalListings":2,"totalPages":1}
```

The villa is over budget; the city matched in lower case. Wrong searches are answered with the reason:

```
$ curl 'localhost:8080/api/listings/search?city=Goa&checkIn=2026-12-23&checkOut=2026-12-20'
{"detail":"checkOut must be after checkIn","instance":"/api/listings/search","status":400,"title":"Bad Request"}

$ curl 'localhost:8080/api/listings/search?city=Goa&size=100&guests=0'
{"detail":"request parameters are invalid","instance":"/api/listings/search","status":400,"title":"Bad Request",
 "errors":{"guests":"must be greater than or equal to 1","size":"must be less than or equal to 50"}}
```

The constraints on the parameters (`@Min(1) @Max(16) int guests` ...) are in
[`ListingController`](../../../airbnb-backend/src/main/java/jbdl/airbnb/listings/ListingController.java);
[`ApiExceptionHandler`](../../../airbnb-backend/src/main/java/jbdl/airbnb/common/ApiExceptionHandler.java)
lists every wrong one (without it the answer only said "Validation failure").

## Tested

[`ListingSearchTest`](../../../airbnb-backend/src/test/java/jbdl/airbnb/ListingSearchTest.java), each
test with a city of its own. Bookings cannot be made through the API before Session 22, so the test
stores them with `BookingRepository`:

- city in any case and guests; budget;
- a listing booked for some of the nights is left out, and found again without dates;
- back to back: bookings that end on our check-in day and start on our check-out day do not block;
- a cancelled booking does not block; a deactivated listing is never found;
- 5 listings, 2 per page, sorted by price: page 1 holds the 3rd and 4th cheapest, 3 pages in total;
- 400 for one date only, check-out before check-in, check-in yesterday, a non-ISO date, an unknown
  sort, size 51, 0 guests.

The test dates are relative to today (30 days ahead), so the tests do not start failing on a later
date.

## Not yet

Two guests can still book the same nights if they press "book" at the same moment: both searches see
the dates free. Preventing that is the first job of Session 22.

## Interview questions

- **How do you find listings free on given dates?** Leave out every listing with a confirmed booking
  that overlaps: `not exists (... b.checkIn < :checkOut and b.checkOut > :checkIn)`, with an index on
  the booking's listing and dates.
- **Offset paging vs. keyset paging?** Offset (`page=200`) makes the database skip all earlier rows,
  which gets slow deep into the results; keyset paging ("after price 3,500 and id 41") stays fast, but
  cannot jump to a page number.
- **Why must the sort order be complete?** Without a unique last key, rows with equal values can come
  back in a different order per query, so pages repeat or miss rows.
- **Why limit the page size?** One request must not be able to make the server load and send
  unlimited rows.
