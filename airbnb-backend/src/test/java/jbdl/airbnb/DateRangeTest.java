package jbdl.airbnb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import jbdl.airbnb.bookings.DateRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The stay rule every booking and search depends on: check-in day included, check-out day not.
 * Plain Java, no Spring. Days are written as "20 Dec" in the comments.
 */
class DateRangeTest {

    private static DateRange stay(int fromDay, int toDay) {
        return new DateRange(LocalDate.of(2026, 12, fromDay), LocalDate.of(2026, 12, toDay));
    }

    @Test
    @DisplayName("20 to 23 Dec is 3 nights (20, 21, 22)")
    void nights() {
        assertThat(stay(20, 23).nights()).isEqualTo(3);
    }

    @Test
    @DisplayName("Overlapping stays: one night shared, one inside the other, the same dates")
    void overlapping() {
        assertThat(stay(20, 23).overlaps(stay(22, 25))).isTrue();   // the night of the 22nd
        assertThat(stay(20, 27).overlaps(stay(22, 24))).isTrue();   // completely inside
        assertThat(stay(20, 23).overlaps(stay(20, 23))).isTrue();
        assertThat(stay(22, 25).overlaps(stay(20, 23))).isTrue();   // the rule works both ways round
    }

    @Test
    @DisplayName("Back to back is NOT an overlap: one guest leaves on the 23rd, the next arrives on the 23rd")
    void backToBack() {
        assertThat(stay(20, 23).overlaps(stay(23, 25))).isFalse();
        assertThat(stay(23, 25).overlaps(stay(20, 23))).isFalse();
    }

    @Test
    @DisplayName("Separate stays do not overlap")
    void separate() {
        assertThat(stay(20, 22).overlaps(stay(24, 26))).isFalse();
    }

    @Test
    @DisplayName("A stay must end after it starts, and needs both dates")
    void invalid() {
        assertThatThrownBy(() -> stay(23, 23))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("checkOut must be after checkIn");
        assertThatThrownBy(() -> stay(23, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DateRange(LocalDate.of(2026, 12, 20), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("checkIn and checkOut are both required");
    }
}
