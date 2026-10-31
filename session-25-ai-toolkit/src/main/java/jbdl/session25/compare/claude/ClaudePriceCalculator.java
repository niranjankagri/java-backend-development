package jbdl.session25.compare.claude;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import jbdl.session25.compare.PriceCalculator;

/**
 * Calculates the total price of a hotel stay.
 *
 * <p>How the price is built:
 * <ol>
 *   <li>Every night from {@code checkIn} (included) to {@code checkOut} (not included) is charged.</li>
 *   <li>A Friday night or a Saturday night costs 20 % more than the nightly price.</li>
 *   <li>A stay of 7 nights or more gets 10 % off the sum of all its nights.</li>
 *   <li>A cleaning fee of 500.00 is added once, after the discount.</li>
 *   <li>Only the final total is rounded: 2 decimals, rounding half up.</li>
 * </ol>
 *
 * <p>All intermediate amounts are kept exact with {@link BigDecimal}, so no rounding
 * happens before the very last step.
 */
public class ClaudePriceCalculator implements PriceCalculator {

    private static final int MAX_NIGHTS = 30;
    private static final int DISCOUNT_MIN_NIGHTS = 7;
    private static final BigDecimal WEEKEND_FACTOR = new BigDecimal("1.20");
    private static final BigDecimal DISCOUNT_FACTOR = new BigDecimal("0.90");
    private static final BigDecimal CLEANING_FEE = new BigDecimal("500.00");

    /**
     * Creates a price calculator. It has no state, so one instance can be reused.
     */
    public ClaudePriceCalculator() {
    }

    @Override
    public BigDecimal total(BigDecimal nightlyPrice, LocalDate checkIn, LocalDate checkOut) {
        if (nightlyPrice == null || checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("No argument may be null");
        }
        if (nightlyPrice.signum() <= 0) {
            throw new IllegalArgumentException("nightlyPrice must be greater than zero");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > MAX_NIGHTS) {
            throw new IllegalArgumentException("A stay has at most " + MAX_NIGHTS + " nights");
        }

        // Add up the price of every night.
        BigDecimal sum = BigDecimal.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            sum = sum.add(priceOfNight(nightlyPrice, night));
        }

        // Long stays get a discount on the nights (not on the cleaning fee).
        if (nights >= DISCOUNT_MIN_NIGHTS) {
            sum = sum.multiply(DISCOUNT_FACTOR);
        }

        BigDecimal total = sum.add(CLEANING_FEE);
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns the price of one night: Friday and Saturday nights cost 20 % more.
     */
    private static BigDecimal priceOfNight(BigDecimal nightlyPrice, LocalDate night) {
        DayOfWeek day = night.getDayOfWeek();
        if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY) {
            return nightlyPrice.multiply(WEEKEND_FACTOR);
        }
        return nightlyPrice;
    }
}
