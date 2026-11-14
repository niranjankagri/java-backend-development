# Session 22: Airbnb Backend System (Part 2)

Part 2 of the Airbnb-like backend in [`airbnb-backend`](../airbnb-backend): guests book stays, and
never two for the same night; users log in; guests review their stays; hosts are told about bookings.

| # | Topic | README |
|---|---|---|
| 1 | Bookings: rules, price, cancelling | [01-bookings](topics/01-bookings/README.md) |
| 2 | No double bookings, even at the same moment | [02-no-double-bookings](topics/02-no-double-bookings/README.md) |
| 3 | Login, and who may change what | [03-login-and-ownership](topics/03-login-and-ownership/README.md) |
| 4 | Reviews and ratings | [04-reviews](topics/04-reviews/README.md) |
| 5 | Booking events and notifications | [05-booking-events](topics/05-booking-events/README.md) |

## What works after this session

- Booking: price fixed at booking time, every rule answered with its reason, cancelling before check-in.
- No double bookings: the listing is locked while a booking is checked and saved; 10 simultaneous
  bookings of the same nights give exactly 1 booking.
- Login with email and password (HTTP Basic, BCrypt). Browsing stays open; changing anything needs a
  login; only a listing's host may change it or see its bookings.
- Reviews after a stay, once per stay, with the listing's average rating.
- The host's notifications, sent only after the booking is committed.
- 45 tests (26 of Session 21, adapted to the login, and 19 new):
  [`BookingApiTest`](../airbnb-backend/src/test/java/jbdl/airbnb/BookingApiTest.java),
  [`DoubleBookingTest`](../airbnb-backend/src/test/java/jbdl/airbnb/DoubleBookingTest.java),
  [`SecurityRulesTest`](../airbnb-backend/src/test/java/jbdl/airbnb/SecurityRulesTest.java),
  [`ReviewApiTest`](../airbnb-backend/src/test/java/jbdl/airbnb/ReviewApiTest.java),
  [`BookingEventsTest`](../airbnb-backend/src/test/java/jbdl/airbnb/BookingEventsTest.java).

## What you can do after this session

- Write booking rules as clear 400 / 403 / 404 / 409 answers.
- Spot a check-then-act race between transactions, fix it with a pessimistic lock, name the
  alternatives, and prove the fix with a concurrent test that fails without it.
- Put an API behind a login, keep browsing open, and order the URL rules correctly.
- Tie reviews to stays and let the database compute the average.
- Publish application events and react to them only after the commit.

## Run it

```bash
./mvnw -pl airbnb-backend verify
java -jar airbnb-backend/target/airbnb-backend-1.0.0-SNAPSHOT.jar
curl -X POST localhost:8080/api/users -H 'Content-Type: application/json' -d '{"name":"Asha","email":"asha@example.com","password":"asha-secret-1"}'
curl -X POST localhost:8080/api/users -H 'Content-Type: application/json' -d '{"name":"Ravi","email":"ravi@example.com","password":"ravi-secret-1"}'
curl -u asha@example.com:asha-secret-1 -X POST localhost:8080/api/listings -H 'Content-Type: application/json' -d '{"title":"Sea view flat","city":"Goa","maxGuests":4,"bedrooms":2,"nightlyPrice":3500}'
curl -u ravi@example.com:ravi-secret-1 -X POST localhost:8080/api/bookings -H 'Content-Type: application/json' -d '{"listingId":1,"checkIn":"2026-12-20","checkOut":"2026-12-23","guests":2}'
curl -u asha@example.com:asha-secret-1 localhost:8080/api/users/me/notifications
```

(Choose check-in and check-out dates in the future.)
