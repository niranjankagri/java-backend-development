-- Session 23: the database itself refuses two CONFIRMED bookings of one listing that share a night.
-- BookingService already prevents that with a row lock (Session 22, topic 2); this is the second line
-- of defence for every other writer of the table (a script, another service, a future bug).
--
-- An EXCLUSION constraint: no two rows may have the same listing_id (=) AND overlapping date ranges (&&).
-- daterange(check_in, check_out) is half-open by default, '[)': check-in day included, check-out day not,
-- exactly the rule of DateRange (Session 21), so back-to-back stays are still allowed.
-- btree_gist lets the GiST index behind the constraint compare plain bigints with "=".

create extension if not exists btree_gist;

alter table booking
    add constraint ex_booking_no_overlap
    exclude using gist (listing_id with =, daterange(check_in, check_out) with &&)
    where (status = 'CONFIRMED');
