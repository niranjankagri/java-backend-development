# Topic 1: Bookings

| Method | URL | Who | Answers |
|---|---|---|---|
| POST | `/api/bookings` | a logged-in guest | 201, 400 dates, 404 listing, 409 a rule |
| POST | `/api/bookings/{id}/cancel` | the guest who booked | 200, 403, 404, 409 |
| GET | `/api/users/me/bookings` | the logged-in guest | 200 |
| GET | `/api/listings/{id}/bookings` | the listing's host | 200, 403 |

## The rules

[`BookingService.book`](../../../airbnb-backend/src/main/java/jbdl/airbnb/bookings/BookingService.java), in
the order they are checked:

| Rule | Answer |
|---|---|
| both dates, check-out after check-in (`DateRange`, Session 21) | 400 |
| check-in today or later; at most 30 nights | 400 |
| the listing exists | 404 |
| the listing is active | 409 "listing 1 is not available" |
| the guest is not its host | 409 "you cannot book your own listing" |
| guests ≤ the listing's maximum | 409 "listing 1 takes at most 4 guests" |
| no confirmed booking overlaps the nights | 409 "listing 1 is already booked for some of these nights" |

400 means the request itself is wrong; 409 means it is fine, but the current state of the data says no.

The price is fixed now and stored: `nightlyPrice × nights`. If the host changes the price tomorrow, this
booking keeps its price (Session 21, topic 3).

## Cancelling

A booking is never deleted; it becomes `CANCELLED` and stays in the history. Search and booking count
only `CONFIRMED` stays, so the nights are free again at once. Rules: only the guest who booked (403),
not twice (409), and only before the check-in day (409). The host cannot cancel a guest's booking; that
would need its own rules (refund, notice period), not built here.

Cancel is `POST /api/bookings/{id}/cancel`, not `DELETE`: nothing is removed, the booking changes state.

## A real run

```
$ curl -u ravi@example.com:ravi-secret-1 -X POST localhost:8080/api/bookings -H 'Content-Type: application/json' -d '{"listingId":1,"checkIn":"2026-11-30","checkOut":"2026-12-03","guests":2}'
{"id":1,"listingId":1,"listingTitle":"Sea view flat","guestId":2,"checkIn":"2026-11-30","checkOut":"2026-12-03","nights":3,"guests":2,"totalPrice":10500.00,"status":"CONFIRMED"}

$ curl -u meera@example.com:meera-secret-1 -X POST localhost:8080/api/bookings -H 'Content-Type: application/json' -d '{"listingId":1,"checkIn":"2026-12-02","checkOut":"2026-12-05","guests":2}'
{"detail":"listing 1 is already booked for some of these nights","instance":"/api/bookings","status":409,"title":"Conflict"}

$ curl -u meera@example.com:meera-secret-1 -X POST localhost:8080/api/bookings/1/cancel
{"detail":"only the guest who booked can cancel booking 1","instance":"/api/bookings/1/cancel","status":403,"title":"Forbidden"}

$ curl -u ravi@example.com:ravi-secret-1 -X POST localhost:8080/api/bookings/1/cancel
{"id":1,...,"status":"CANCELLED"}

$ curl -u meera@example.com:meera-secret-1 -X POST localhost:8080/api/bookings -H 'Content-Type: application/json' -d '{"listingId":1,"checkIn":"2026-12-02","checkOut":"2026-12-05","guests":2}'
{"id":2,"listingId":1,"listingTitle":"Sea view flat","guestId":3,"checkIn":"2026-12-02","checkOut":"2026-12-05","nights":3,"guests":2,"totalPrice":10500.00,"status":"CONFIRMED"}
```

## Tested

[`BookingApiTest`](../../../airbnb-backend/src/test/java/jbdl/airbnb/BookingApiTest.java): the price and
nights; a second guest for one shared night is 409, back to back is fine; own listing, too many guests,
deactivated and unknown listing; the 400 date rules; booked dates disappear from search (Session 21's
query); cancelling frees the nights; cancel rules (someone else, the host, twice, on the check-in day);
who sees which bookings.

## Interview questions

- **400 or 409 for "already booked"?** 409 Conflict: the request is valid, it conflicts with the current
  state. 400 is for requests that could never succeed as written.
- **Why store the booking's total?** It is what the guest agreed to; recomputing it from today's price
  would change the past.
- **Why not DELETE to cancel?** The booking stays (history, refunds, reviews); only its state changes.
