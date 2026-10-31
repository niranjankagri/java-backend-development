package jbdl.session27.refund;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Topic 4: the acceptance tests of topics/02-writing-a-spec/SPEC.md. Each group is named after the
 * rule it checks (R1 to R7), so a failing test points at the line of the specification it breaks.
 * The tested class was generated from the specification (topic 3, run C1) and copied unchanged.
 */
class StandardRefundPolicyTest {

    // the booking of the SPEC.md examples
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 12, 20);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 12, 23);
    private static final Booking BOOKING =
            new Booking(new BigDecimal("10000.00"), new BigDecimal("500.00"), CHECK_IN, CHECK_OUT);

    private final RefundPolicy policy = new StandardRefundPolicy();

    @DisplayName("SPEC.md examples")
    @ParameterizedTest(name = "{1} on {0} -> {2}")
    @CsvSource({
            "2026-12-01, GUEST, 10500.00",
            "2026-12-15, GUEST, 5500.00",
            "2026-12-19, GUEST, 500.00",
            "2026-12-21, GUEST, 0.00",
            "2026-12-19, HOST,  10500.00"
    })
    void specExamples(LocalDate cancelledOn, CancelledBy by, String refund) {
        // JUnit turns the text "2026-12-01" into a LocalDate and "GUEST" into the enum by itself
        assertThat(policy.refund(BOOKING, cancelledOn, by)).isEqualTo(new BigDecimal(refund));
    }

    @Nested
    @DisplayName("R1: invalid input")
    class R1 {

        @Test
        void nullArgumentsAreRejected() {
            assertThatThrownBy(() -> policy.refund(null, CHECK_IN, CancelledBy.GUEST))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> policy.refund(BOOKING, null, CancelledBy.GUEST))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> policy.refund(BOOKING, CHECK_IN, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void aNullFieldOfTheBookingIsRejected() {
            Booking noCheckIn = new Booking(new BigDecimal("10000.00"), new BigDecimal("500.00"), null, CHECK_OUT);
            assertThatThrownBy(() -> policy.refund(noCheckIn, LocalDate.of(2026, 12, 1), CancelledBy.GUEST))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void negativeAmountsAreRejected() {
            Booking negative = new Booking(new BigDecimal("10000.00"), new BigDecimal("-1.00"), CHECK_IN, CHECK_OUT);
            assertThatThrownBy(() -> policy.refund(negative, LocalDate.of(2026, 12, 1), CancelledBy.GUEST))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void checkOutMustBeAfterCheckIn() {
            Booking zeroNights = new Booking(new BigDecimal("10000.00"), new BigDecimal("500.00"), CHECK_IN, CHECK_IN);
            assertThatThrownBy(() -> policy.refund(zeroNights, LocalDate.of(2026, 12, 1), CancelledBy.GUEST))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("R2: no cancellation on or after check-out")
    class R2 {

        @Test
        void notOnTheCheckOutDayNorLater() {
            assertThatThrownBy(() -> policy.refund(BOOKING, CHECK_OUT, CancelledBy.GUEST))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> policy.refund(BOOKING, CHECK_OUT.plusDays(7), CancelledBy.GUEST))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("R3 and R4: the host cancels")
    class HostCancels {

        @Test
        void beforeTheCheckInDayTheGuestGetsEverythingBack() {
            assertThat(policy.refund(BOOKING, LocalDate.of(2026, 11, 20), CancelledBy.HOST))
                    .isEqualTo(new BigDecimal("10500.00"));
        }

        @Test
        void onOrAfterTheCheckInDayItIsNotAllowed() {
            assertThatThrownBy(() -> policy.refund(BOOKING, CHECK_IN, CancelledBy.HOST))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> policy.refund(BOOKING, CHECK_IN.plusDays(1), CancelledBy.HOST))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("R5 and R6: the guest cancels, the boundaries")
    class GuestCancels {

        @ParameterizedTest(name = "{0} days before check-in -> {1}")
        @CsvSource({
                "7, 10500.00",   // 7 or more: all nights + cleaning fee
                "6, 5500.00",    // 2 to 6: half the nights + cleaning fee
                "2, 5500.00",
                "1, 500.00",     // 1: no nights, but the cleaning fee (still before the check-in day)
                "0, 0.00",       // the check-in day: nothing
                "-2, 0.00"       // during the stay: nothing
        })
        void refundByDaysBeforeCheckIn(int daysBefore, String refund) {
            LocalDate cancelledOn = CHECK_IN.minusDays(daysBefore);
            assertThat(policy.refund(BOOKING, cancelledOn, CancelledBy.GUEST)).isEqualTo(new BigDecimal(refund));
        }

        @Test
        void withoutACleaningFeeOnlyTheNightsCount() {
            Booking noFee = new Booking(new BigDecimal("10000.00"), new BigDecimal("0.00"), CHECK_IN, CHECK_OUT);
            assertThat(policy.refund(noFee, CHECK_IN.minusDays(10), CancelledBy.GUEST))
                    .isEqualTo(new BigDecimal("10000.00"));
        }
    }

    @Nested
    @DisplayName("R7: rounding")
    class R7 {

        @Test
        void halfOfAnOddAmountIsRoundedHalfUpOnce() {
            // 50 % of 1000.05 = 500.025 -> 500.03
            Booking odd = new Booking(new BigDecimal("1000.05"), new BigDecimal("0.00"), CHECK_IN, CHECK_OUT);
            assertThat(policy.refund(odd, CHECK_IN.minusDays(3), CancelledBy.GUEST))
                    .isEqualTo(new BigDecimal("500.03"));
        }

        @Test
        void theResultAlwaysHasTwoDecimals() {
            // isEqualTo on BigDecimal compares the scale too: 0 and 0.00 are not equal
            assertThat(policy.refund(BOOKING, CHECK_IN, CancelledBy.GUEST).scale()).isEqualTo(2);
        }
    }
}
