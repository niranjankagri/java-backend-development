import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Quotes the cost of a stay.
 * Usage: java PriceQuote <nightly-price> <check-in YYYY-MM-DD> <check-out YYYY-MM-DD>
 *
 * A night is charged to the date it starts on, so a stay from Friday to Monday
 * has nights for Friday, Saturday and Sunday. Nights starting on Saturday or
 * Sunday cost 20% more than the nightly price.
 */
public class PriceQuote {

    private static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.20");

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java PriceQuote <nightly-price> <check-in YYYY-MM-DD> <check-out YYYY-MM-DD>");
            System.exit(1);
        }

        BigDecimal nightly;
        LocalDate checkIn;
        LocalDate checkOut;
        try {
            nightly = new BigDecimal(args[0]).setScale(2, RoundingMode.HALF_UP);
            checkIn = LocalDate.parse(args[1]);
            checkOut = LocalDate.parse(args[2]);
        } catch (IllegalArgumentException e) {
            System.err.println("Could not read input: " + e.getMessage());
            System.exit(1);
            return;
        }

        if (nightly.signum() < 0) {
            System.err.println("The nightly price cannot be negative.");
            System.exit(1);
        }
        if (!checkOut.isAfter(checkIn)) {
            System.err.println("Check-out must be after check-in.");
            System.exit(1);
        }

        int weekdayNights = 0;
        int weekendNights = 0;
        for (LocalDate day = checkIn; day.isBefore(checkOut); day = day.plusDays(1)) {
            DayOfWeek dow = day.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                weekendNights++;
            } else {
                weekdayNights++;
            }
        }

        BigDecimal weekendRate = nightly.multiply(WEEKEND_MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
        BigDecimal weekdayTotal = nightly.multiply(BigDecimal.valueOf(weekdayNights));
        BigDecimal weekendTotal = weekendRate.multiply(BigDecimal.valueOf(weekendNights));
        BigDecimal total = weekdayTotal.add(weekendTotal);

        System.out.println("Stay: " + checkIn + " to " + checkOut
                + " (" + (weekdayNights + weekendNights) + " nights)");
        System.out.println("Weekday nights: " + weekdayNights + " x " + nightly + " = " + weekdayTotal);
        System.out.println("Weekend nights: " + weekendNights + " x " + weekendRate + " = " + weekendTotal);
        System.out.println("Total: " + total);
    }
}
