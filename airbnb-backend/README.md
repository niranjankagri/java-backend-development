# Airbnb backend

An Airbnb-like rental backend in Spring Boot 4, built over Sessions 21 to 24 of the course:

| Session | Adds |
|---|---|
| [21](../session-21-airbnb-part-1/README.md) | requirements, architecture, entities and schema; users; listings; search with availability |
| [22](../session-22-airbnb-part-2/README.md) | bookings without double bookings, login, reviews, events |
| 23, 24 | deployment |

## API

Open to everyone: register, and browsing (search, listings, a host's listings, reviews). Everything else
needs a login: HTTP Basic with email and password (`curl -u email:password ...`).

| Method | URL | What | Login |
|---|---|---|---|
| POST | `/api/users` | register `{"name","email","password"}` | no |
| GET | `/api/users/me` | who am I | yes |
| GET | `/api/users/{id}` | a user | yes |
| GET | `/api/users/{id}/listings` | a host's listings | no |
| POST | `/api/listings` | create a listing; the logged-in user is the host | yes |
| GET | `/api/listings/{id}` | a listing | no |
| PUT | `/api/listings/{id}` | change it (only its host) | yes |
| DELETE | `/api/listings/{id}` | deactivate it (only its host) | yes |
| GET | `/api/listings/search` | `city` (required), `guests`, `maxPrice`, `checkIn` + `checkOut`, `sort` (`newest`, `price`, `price_desc`), `page`, `size` (max 50) | no |
| POST | `/api/bookings` | book `{"listingId","checkIn","checkOut","guests"}` | yes |
| POST | `/api/bookings/{id}/cancel` | cancel (only the guest, before check-in) | yes |
| GET | `/api/users/me/bookings` | my bookings as a guest | yes |
| GET | `/api/listings/{id}/bookings` | a listing's bookings (only its host) | yes |
| POST | `/api/bookings/{id}/review` | review a stay `{"rating","comment"}` (only the guest, after check-out, once) | yes |
| GET | `/api/listings/{id}/reviews` | reviews and average rating | no |
| GET | `/api/users/me/notifications` | my notifications (hosts: bookings and cancellations) | yes |

Errors are Problem Details: 400 (with `errors` per field or parameter), 401, 403, 404, 409.

## Run it

```bash
./mvnw -pl airbnb-backend verify                                     # 45 tests
java -jar airbnb-backend/target/airbnb-backend-1.0.0-SNAPSHOT.jar    # http://localhost:8080
```

H2 in memory: the data is gone when the application stops. PostgreSQL comes with deployment.

## Layout

```
src/main/java/jbdl/airbnb/
  AirbnbApplication.java
  users/          AppUser, UserService, UserController, UserDtos
  listings/       Listing, Amenity, ListingRepository (search queries, locking read), ListingService, ListingController, ListingDtos
  bookings/       Booking, BookingStatus, BookingRepository, DateRange, BookingService, BookingController, BookingDtos, BookingEvents
  reviews/        Review, ReviewRepository, ReviewService, ReviewController, ReviewDtos
  notifications/  HostNotifier (after-commit listener), NotificationInbox, NotificationController
  security/       SecurityConfig, EmailUserDetailsService
  common/         NotFoundException, ForbiddenException, BusinessRuleException, ApiExceptionHandler
src/test/java/jbdl/airbnb/
  DateRangeTest, SchemaDesignTest, UserAndListingApiTest, ListingSearchTest          (Session 21)
  BookingApiTest, DoubleBookingTest, SecurityRulesTest, ReviewApiTest, BookingEventsTest   (Session 22)
  ApiTestSupport
```
