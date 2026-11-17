package jbdl.session25.compare.codex;

import jbdl.session25.compare.PriceCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Calculates the total price of a stay using weekend, long-stay, and cleaning-fee rules. */
public class CodexPriceCalculator implements PriceCalculator {

    private static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal LONG_STAY_MULTIPLIER = new BigDecimal("0.90");
    private static final BigDecimal CLEANING_FEE = new BigDecimal("500.00");

    @Override
    public BigDecimal total(BigDecimal nightlyPrice, LocalDate checkIn, LocalDate checkOut) {
        if (nightlyPrice == null || checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Arguments must not be null");
        }
        if (nightlyPrice.signum() <= 0) {
            throw new IllegalArgumentException("Nightly price must be greater than zero");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > 30) {
            throw new IllegalArgumentException("A stay must not exceed 30 nights");
        }

        BigDecimal nightlyTotal = BigDecimal.ZERO;
        for (long offset = 0; offset < nights; offset++) {
            LocalDate night = checkIn.plusDays(offset);
            DayOfWeek day = night.getDayOfWeek();
            if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY) {
                nightlyTotal = nightlyTotal.add(nightlyPrice.multiply(WEEKEND_MULTIPLIER));
            } else {
                nightlyTotal = nightlyTotal.add(nightlyPrice);
            }
        }

        if (nights >= 7) {
            nightlyTotal = nightlyTotal.multiply(LONG_STAY_MULTIPLIER);
        }

        return nightlyTotal.add(CLEANING_FEE).setScale(2, RoundingMode.HALF_UP);
    }
}
