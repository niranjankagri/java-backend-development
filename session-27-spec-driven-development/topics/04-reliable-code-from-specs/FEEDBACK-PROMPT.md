Write the class StandardRefundPolicy in package jbdl.session27.refund for our booking app. It
implements this interface and uses these types:

```java
public record Booking(BigDecimal nightsTotal, BigDecimal cleaningFee, LocalDate checkIn, LocalDate checkOut) {}

public enum CancelledBy { GUEST, HOST }

public interface RefundPolicy {
    /** The amount refunded to the guest, in rupees with 2 decimals. */
    BigDecimal refund(Booking booking, LocalDate cancelledOn, CancelledBy cancelledBy);
}
```

Guests get a full refund if they cancel a week or more before check-in, half if they cancel a few
days before, and nothing if they cancel at the last minute. If the host cancels, the guest gets
everything back. The cleaning fee should be refunded if the guest never arrives. Handle bad input
sensibly. Java 17, only the JDK.

Answer with only one Java code block with the complete class, no explanation.

This is your first answer:

```java
package jbdl.session27.refund;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class StandardRefundPolicy implements RefundPolicy {

    /** Guest cancellations at least this many days before check-in get the nights refunded in full. */
    static final long FULL_REFUND_MIN_DAYS = 7;

    /** Guest cancellations at least this many days before check-in (but under 7) get half the nights back. */
    static final long HALF_REFUND_MIN_DAYS = 3;

    private static final BigDecimal HALF = new BigDecimal("0.5");
    private static final int SCALE = 2;

    @Override
    public BigDecimal refund(Booking booking, LocalDate cancelledOn, CancelledBy cancelledBy) {
        Objects.requireNonNull(booking, "booking must not be null");
        Objects.requireNonNull(cancelledOn, "cancelledOn must not be null");
        Objects.requireNonNull(cancelledBy, "cancelledBy must not be null");

        BigDecimal nights = Objects.requireNonNull(booking.nightsTotal(), "nightsTotal must not be null");
        BigDecimal cleaning = Objects.requireNonNull(booking.cleaningFee(), "cleaningFee must not be null");
        LocalDate checkIn = Objects.requireNonNull(booking.checkIn(), "checkIn must not be null");
        LocalDate checkOut = Objects.requireNonNull(booking.checkOut(), "checkOut must not be null");

        if (nights.signum() < 0) {
            throw new IllegalArgumentException("nightsTotal must not be negative: " + nights);
        }
        if (cleaning.signum() < 0) {
            throw new IllegalArgumentException("cleaningFee must not be negative: " + cleaning);
        }
        if (checkOut.isBefore(checkIn)) {
            throw new IllegalArgumentException("checkOut " + checkOut + " is before checkIn " + checkIn);
        }

        // Host cancellation: guest gets everything back.
        if (cancelledBy == CancelledBy.HOST) {
            return money(nights.add(cleaning));
        }

        // Guest cancellation (the only other CancelledBy value).
        long daysBeforeCheckIn = ChronoUnit.DAYS.between(cancelledOn, checkIn);

        BigDecimal nightsRefund;
        if (daysBeforeCheckIn >= FULL_REFUND_MIN_DAYS) {
            nightsRefund = nights;
        } else if (daysBeforeCheckIn >= HALF_REFUND_MIN_DAYS) {
            nightsRefund = nights.multiply(HALF);
        } else {
            // Last-minute, or cancelled after check-in: nothing for the nights.
            nightsRefund = BigDecimal.ZERO;
        }

        // Guest never arrives when cancelling before check-in day, so the cleaning fee comes back.
        BigDecimal cleaningRefund = daysBeforeCheckIn > 0 ? cleaning : BigDecimal.ZERO;

        return money(nightsRefund.add(cleaningRefund));
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
```

Our acceptance tests gave this result:

```
PASSED 9/17
  FAIL guest 2 days before: 500.00 (expected 5500.00)
  FAIL guest on check-out day: 0.00 (expected IllegalArgumentException)
  FAIL guest after check-out: 0.00 (expected IllegalArgumentException)
  FAIL host during the stay: 10500.00 (expected IllegalArgumentException)
  FAIL null cancelledBy: NullPointerException (expected IllegalArgumentException)
  FAIL null booking: NullPointerException (expected IllegalArgumentException)
  FAIL check-out not after check-in: 10500.00 (expected IllegalArgumentException)
  FAIL null check-in: NullPointerException (expected IllegalArgumentException)
```

Fix the class so that all acceptance tests pass. Answer with only one Java code block with the complete class, no explanation.
