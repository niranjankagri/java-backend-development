package jbdl.airbnb.bookings;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A stay: from the check-in day up to, but NOT including, the check-out day. Checking out on the 23rd
 * frees the 23rd for the next guest's check-in. That "half-open" rule makes two things simple:
 * <ul>
 *   <li>nights = days between the two dates (20th to 23rd = 3 nights: 20, 21, 22)</li>
 *   <li>two stays overlap when each starts before the other ends (see {@link #overlaps})</li>
 * </ul>
 * Search uses it now (Session 21); booking uses it from Session 22.
 */
public record DateRange(LocalDate checkIn, LocalDate checkOut) {

    /** A record's "compact constructor": runs before the fields are set, so a wrong range never exists. */
    public DateRange {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("checkIn and checkOut are both required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
    }

    public long nights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /**
     * True if the two stays share at least one night. The same rule is written in SQL in
     * ListingRepository.searchAvailable: b.checkIn &lt; :checkOut and b.checkOut &gt; :checkIn.
     */
    public boolean overlaps(DateRange other) {
        return checkIn.isBefore(other.checkOut) && checkOut.isAfter(other.checkIn);
    }
}
