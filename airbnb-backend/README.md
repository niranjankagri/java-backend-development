# Airbnb backend

An Airbnb-like rental backend in Spring Boot 4, built over Sessions 21 to 24 of the course:

| Session | Adds |
|---|---|
| [21](../session-21-airbnb-part-1/README.md) | requirements, architecture, entities and schema; users; listings; search with availability |
| 22 | bookings without double bookings, login, reviews, events |
| 23, 24 | deployment |

## API (Session 21)

| Method | URL | What |
|---|---|---|
| POST | `/api/users` | register `{"name","email"}` |
| GET | `/api/users/{id}` | a user |
| GET | `/api/users/{id}/listings` | a host's listings |
| POST | `/api/listings` | create a listing (`hostId` in the body until login arrives) |
| GET | `/api/listings/{id}` | a listing |
| PUT | `/api/listings/{id}` | change it (only its host) |
| DELETE | `/api/listings/{id}?hostId=` | deactivate it (only its host) |
| GET | `/api/listings/search` | `city` (required), `guests`, `maxPrice`, `checkIn` + `checkOut`, `sort` (`newest`, `price`, `price_desc`), `page`, `size` (max 50) |

Errors are Problem Details: 400 (with `errors` per field or parameter), 403, 404, 409.

## Run it

```bash
./mvnw -pl airbnb-backend verify                                     # 26 tests
java -jar airbnb-backend/target/airbnb-backend-1.0.0-SNAPSHOT.jar    # http://localhost:8080
```

H2 in memory: the data is gone when the application stops. PostgreSQL comes with deployment.

## Layout

```
src/main/java/jbdl/airbnb/
  AirbnbApplication.java
  users/       AppUser, UserService, UserController, UserDtos
  listings/    Listing, Amenity, ListingRepository (search queries), ListingService, ListingController, ListingDtos
  bookings/    Booking, BookingStatus, BookingRepository, DateRange (the stay and overlap rule)
  common/      NotFoundException, ForbiddenException, BusinessRuleException, ApiExceptionHandler
src/test/java/jbdl/airbnb/
  DateRangeTest, SchemaDesignTest, UserAndListingApiTest, ListingSearchTest, ApiTestSupport
```
