package jbdl.airbnb.bookings;

import org.springframework.data.jpa.repository.JpaRepository;

/** Used by the tests in Session 21 (to put bookings in the way of a search); the booking API comes in Session 22. */
public interface BookingRepository extends JpaRepository<Booking, Long> {
}
