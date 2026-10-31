package jbdl.airbnb.listings;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * The two search queries are JPQL (Session 11): they name entities and fields (Listing, nightlyPrice),
 * not tables and columns. Spring Data adds the paging (LIMIT/OFFSET), the sorting, and a second query
 * that counts all matches for the page numbers.
 */
public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByHostIdOrderByIdAsc(long hostId);

    /** Search without dates: active listings in a city that fit the guests and the budget. */
    @Query("""
            select l from Listing l
            where l.active = true
              and lower(l.city) = lower(:city)
              and l.maxGuests >= :guests
              and l.nightlyPrice <= :maxPrice
            """)
    Page<Listing> search(String city, int guests, BigDecimal maxPrice, Pageable pageable);

    /**
     * The same, and the listing must be free on the dates: no CONFIRMED booking overlaps them.
     * Overlap of two stays (check-out day not counted, see DateRange): each starts before the other ends.
     */
    @Query("""
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
            """)
    Page<Listing> searchAvailable(String city, int guests, BigDecimal maxPrice, LocalDate checkIn,
                                  LocalDate checkOut, Pageable pageable);
}
