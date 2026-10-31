# Topic 3: The database as the second guard against double bookings

Session 22 prevents double bookings in the code: the listing row is locked while a booking is checked
and saved. Session 22 also named the PostgreSQL way to make the **database** refuse them. With
PostgreSQL in production, it is now one migration
([`V2__no_overlapping_bookings.sql`](../../../airbnb-backend/src/main/resources/db/migration/V2__no_overlapping_bookings.sql)):

```sql
create extension if not exists btree_gist;

alter table booking
    add constraint ex_booking_no_overlap
    exclude using gist (listing_id with =, daterange(check_in, check_out) with &&)
    where (status = 'CONFIRMED');
```

- An **exclusion constraint** forbids two rows for which all the listed comparisons are true: the same
  `listing_id` (`=`) **and** overlapping date ranges (`&&`).
- `daterange(check_in, check_out)` is half-open, `[check_in, check_out)`: the same rule as `DateRange`
  (Session 21). Back-to-back stays do not overlap.
- `where (status = 'CONFIRMED')`: cancelled bookings do not count, as in the code.
- `btree_gist` lets the index behind the constraint handle the plain `bigint` with `=`. Managed
  PostgreSQL services normally allow this extension; check before relying on it.

## A real run

Inserting, with plain SQL past the application, the same nights the API had already booked:

```
$ docker compose exec postgres psql -U airbnb -d airbnb -c "insert into booking (...) values (1, 2, '2026-11-30', '2026-12-03', 2, 1, 'CONFIRMED', now())"
ERROR:  conflicting key value violates exclusion constraint "ex_booking_no_overlap"
DETAIL:  Key (listing_id, daterange(check_in, check_out))=(1, [2026-11-30,2026-12-03)) conflicts with existing key (listing_id, daterange(check_in, check_out))=(1, [2026-11-30,2026-12-03)).
```

## Why keep the lock as well?

To find out, the lock was removed for one run of the 10-guest race test on PostgreSQL. The constraint
still let only one booking in: the other attempts failed with `ex_booking_no_overlap`. But those
guests did not get the clear answer "listing 1 is already booked for some of these nights"; they got a
database error. So:

| Guard | Gives |
|---|---|
| the lock + check in `BookingService` | the right answer for the user (409 with the reason), on any database |
| the exclusion constraint | a guarantee for every writer of the table: scripts, other services, future bugs |

If the constraint ever fires through the API, [`ApiExceptionHandler`](../../../airbnb-backend/src/main/java/jbdl/airbnb/common/ApiExceptionHandler.java)
answers 409 "the request conflicts with existing data" (new in this session), not 500, and does not pass
the database's message, with its table and column names, to the client.

## Tested

[`ProdProfileTest`](../../../airbnb-backend/src/test/java/jbdl/airbnb/ProdProfileTest.java), on
PostgreSQL 18: an overlapping confirmed booking inserted with SQL is refused (`ex_booking_no_overlap`),
a back-to-back one and an overlapping cancelled one are accepted; and the 10-guest race through
`BookingService` gives exactly 1 booking (the lock is `SELECT ... FOR UPDATE` on PostgreSQL too).

## Interview questions

- **What is an exclusion constraint?** A generalised unique constraint: no two rows may match on all the
  given operators (here equal listing and overlapping ranges).
- **Why both a lock and a constraint?** The lock gives a good answer; the constraint gives a guarantee
  that does not depend on every writer using the code.
- **Why must the range be half-open?** So that a stay ending on the 23rd and one starting on the 23rd do
  not count as overlapping.
