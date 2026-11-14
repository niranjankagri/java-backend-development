package jbdl.airbnb.bookings;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Is any CONFIRMED booking of this listing overlapping the stay? The same overlap rule as
     * DateRange.overlaps and the search query (Session 21, topic 3).
     */
    @Query("""
            select count(b) > 0 from Booking b
            where b.listing.id = :listingId
              and b.status = jbdl.airbnb.bookings.BookingStatus.CONFIRMED
              and b.checkIn < :checkOut
              and b.checkOut > :checkIn
            """)
    boolean existsOverlapping(long listingId, LocalDate checkIn, LocalDate checkOut);

    /** A guest's bookings, the next stay first. */
    List<Booking> findByGuestIdOrderByCheckInAsc(long guestId);

    /** The bookings of one listing (for its host), the next stay first. */
    List<Booking> findByListingIdOrderByCheckInAsc(long listingId);
}
