package jbdl.session27.refund;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calculates how much money a guest gets back when a booking is cancelled.
 *
 * <p>The rules:
 * <ol>
 *   <li>Input check: no argument and no field of the booking may be null. The money amounts may
 *       not be negative, and {@code checkOut} must be after {@code checkIn}. Otherwise an
 *       {@link IllegalArgumentException} is thrown.</li>
 *   <li>A booking cannot be cancelled on or after its check-out day.</li>
 *   <li>Host cancels before the check-in day: the guest gets the nights and the cleaning fee back.</li>
 *   <li>Host cancels on or after the check-in day: an exception is thrown, because support
 *       handles a stay that has already started.</li>
 *   <li>Guest cancels: the nights part depends on the days before check-in.
 *       7 or more days: 100 %. 2 to 6 days: 50 %. 1 or fewer days (also during the stay): 0 %.</li>
 *   <li>Guest cancels before the check-in day: the whole cleaning fee is refunded too.
 *       On or after the check-in day: none of the cleaning fee.</li>
 *   <li>The parts are added first, and only the final sum is rounded: 2 decimals, rounding half up.</li>
 * </ol>
 *
 * <p>"Days before check-in" is the number of days from the cancellation day to the check-in day:
 * 1 on the day before check-in, 0 on the check-in day, and negative during the stay.
 *
 * <p>All money is kept in {@link BigDecimal}, so no rounding happens before the very last step.
 */
public class StandardRefundPolicy implements RefundPolicy {

    private static final int MIN_DAYS_FULL_NIGHTS_REFUND = 7;
    private static final int MIN_DAYS_HALF_NIGHTS_REFUND = 2;
    private static final int MIN_DAYS_BEFORE_CHECK_IN = 1;
    private static final BigDecimal FULL_FACTOR = BigDecimal.ONE;
    private static final BigDecimal HALF_FACTOR = new BigDecimal("0.50");
    private static final int MONEY_SCALE = 2;

    /**
     * Creates a refund policy. It has no state, so one instance can be reused.
     */
    public StandardRefundPolicy() {
    }

    @Override
    public BigDecimal refund(Booking booking, LocalDate cancelledOn, CancelledBy cancelledBy) {
        // Step 1: check the input first. Stop early if something is wrong.
        checkInput(booking, cancelledOn, cancelledBy);

        // Step 2: a booking whose stay is over cannot be cancelled.
        if (!cancelledOn.isBefore(booking.checkOut())) {
            throw new IllegalArgumentException(
                    "A booking cannot be cancelled on or after its check-out day");
        }

        // Step 3: count the days from the cancellation to the check-in day.
        long daysBeforeCheckIn = ChronoUnit.DAYS.between(cancelledOn, booking.checkIn());

        // Step 4: the host and the guest have different rules.
        BigDecimal refund;
        if (cancelledBy == CancelledBy.HOST) {
            refund = hostRefund(booking, daysBeforeCheckIn);
        } else {
            refund = guestRefund(booking, daysBeforeCheckIn);
        }

        // Step 5: round only once, at the very end, to 2 decimals.
        return refund.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Throws an exception if any argument or booking field is missing or makes no sense.
     */
    private static void checkInput(Booking booking, LocalDate cancelledOn, CancelledBy cancelledBy) {
        if (booking == null || cancelledOn == null || cancelledBy == null) {
            throw new IllegalArgumentException("No argument may be null");
        }
        if (booking.nightsTotal() == null || booking.cleaningFee() == null
                || booking.checkIn() == null || booking.checkOut() == null) {
            throw new IllegalArgumentException("No field of the booking may be null");
        }
        // signum() is -1 for a negative number, 0 for zero, and 1 for a positive number.
        if (booking.nightsTotal().signum() < 0 || booking.cleaningFee().signum() < 0) {
            throw new IllegalArgumentException("Money amounts may not be negative");
        }
        if (!booking.checkOut().isAfter(booking.checkIn())) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
    }

    /**
     * Returns the host refund. The host may only cancel before the check-in day.
     */
    private static BigDecimal hostRefund(Booking booking, long daysBeforeCheckIn) {
        if (daysBeforeCheckIn >= MIN_DAYS_BEFORE_CHECK_IN) {
            return booking.nightsTotal().add(booking.cleaningFee());
        }
        throw new IllegalArgumentException(
                "A host cannot cancel on or after the check-in day");
    }

    /**
     * Returns the guest refund: part of the nights, plus the cleaning fee if it is early enough.
     */
    private static BigDecimal guestRefund(Booking booking, long daysBeforeCheckIn) {
        BigDecimal nightsPart = nightsRefund(booking.nightsTotal(), daysBeforeCheckIn);

        // The cleaning fee is refunded only when the cancellation is before the check-in day.
        BigDecimal cleaningPart = BigDecimal.ZERO;
        if (daysBeforeCheckIn >= MIN_DAYS_BEFORE_CHECK_IN) {
            cleaningPart = booking.cleaningFee();
        }

        return nightsPart.add(cleaningPart);
    }

    /**
     * Returns the part of the nights that is refunded, based on the days before check-in.
     */
    private static BigDecimal nightsRefund(BigDecimal nightsTotal, long daysBeforeCheckIn) {
        if (daysBeforeCheckIn >= MIN_DAYS_FULL_NIGHTS_REFUND) {
            return nightsTotal.multiply(FULL_FACTOR);
        }
        if (daysBeforeCheckIn >= MIN_DAYS_HALF_NIGHTS_REFUND) {
            return nightsTotal.multiply(HALF_FACTOR);
        }
        // 1 or fewer days before check-in, including during the stay: nothing back.
        return BigDecimal.ZERO;
    }
}
