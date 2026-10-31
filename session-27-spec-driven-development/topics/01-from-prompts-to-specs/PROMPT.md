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
