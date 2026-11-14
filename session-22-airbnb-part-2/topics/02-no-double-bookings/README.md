# Topic 2: No double bookings, even at the same moment

Requirement N1 of Session 21: two guests must never get the same night. The overlap check of topic 1
is not enough on its own.

## The race

Ravi and Meera press "book" for the same nights at the same moment. Each request runs on its own
thread, in its own transaction:

```
Ravi's transaction                         Meera's transaction
  overlapping booking? no                    overlapping booking? no      <- both look before either saves
  save booking                               save booking
  commit                                     commit                       -> two bookings for the same nights
```

Each check is correct for what it saw; the problem is the gap between checking and saving. This is a
**check-then-act** race (Session 5), and here it happens across transactions in the database.

## The fix: lock the listing first

[`ListingRepository.findByIdForUpdate`](../../../airbnb-backend/src/main/java/jbdl/airbnb/listings/ListingRepository.java):

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select l from Listing l where l.id = :id")
Optional<Listing> findByIdForUpdate(long id);
```

`PESSIMISTIC_WRITE` makes Hibernate read the row with `SELECT ... FOR UPDATE`: the row stays locked
until the transaction ends. [`BookingService.book`](../../../airbnb-backend/src/main/java/jbdl/airbnb/bookings/BookingService.java)
takes that lock **before** the overlap check, all in one `@Transactional` method:

```
Ravi's transaction                         Meera's transaction
  lock listing 1                             lock listing 1 ... waits
  overlapping booking? no                           .
  save booking, commit (lock released)              .
                                             (gets the lock)
                                             overlapping booking? YES -> 409
```

Only bookings of the **same** listing wait for each other; bookings of different listings run in
parallel. The lock is held only for a few milliseconds.

The locked read is a method of `ListingService` (`lockForBooking`), not a call to the listing repository
from the booking code: the parts still talk through their services (Session 21, topic 2). It joins the
caller's transaction, so the lock lasts until the booking is committed.

## Proved by a test, and by breaking it

[`DoubleBookingTest`](../../../airbnb-backend/src/test/java/jbdl/airbnb/DoubleBookingTest.java): 10 guests
book the same flat for the same nights. 10 threads wait at a `CountDownLatch` and are released together;
exactly 1 booking must succeed and 9 must be refused, and the database must hold exactly 1 confirmed
booking.

To be sure the test can catch the race, the lock was replaced by a plain `findById` for three runs:
**all 10 bookings succeeded every time** (`expected: 1 but was: 10`). With the lock, 1 succeeds.

## Other ways to the same goal

| Way | How | Trade-off |
|---|---|---|
| **Pessimistic lock** (used here) | lock the listing row, then check and insert | simple, works on every database; bookings of one listing wait in line |
| Optimistic lock | `@Version` on the listing, increment it with every booking; the second commit fails and is retried | no waiting, but retries under contention |
| Database constraint | PostgreSQL exclusion constraint: `EXCLUDE USING gist (listing_id WITH =, daterange(check_in, check_out) WITH &&) WHERE (status = 'CONFIRMED')` | the database itself refuses overlaps, whatever code writes; PostgreSQL only |
| One row per night | a table `booked_night (listing_id, night)` with a unique key | any database refuses a second booking of a night; more rows |

The PostgreSQL constraint is a good second line of defence once the app runs on PostgreSQL (Sessions 23
and 24).

## Interview questions

- **What is a check-then-act race, and where is it here?** Checking "free?" and saving are two steps;
  another transaction can do the same between them. Fix: make the two steps atomic (lock, constraint).
- **Pessimistic vs. optimistic locking?** Pessimistic waits for a lock before reading; optimistic reads
  freely and detects a conflicting change at commit (version number), then retries.
- **Does `@Transactional` alone prevent double bookings?** No. With the usual READ COMMITTED isolation
  both transactions see "no overlapping booking"; a lock, a constraint or SERIALIZABLE isolation is
  needed.
- **How do you test a race condition?** Many threads released at the same moment (a latch), assertions on
  the final state, and a check that the test fails without the fix.
