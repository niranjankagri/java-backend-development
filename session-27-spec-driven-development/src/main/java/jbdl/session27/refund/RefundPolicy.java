package jbdl.session27.refund;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * How much money a guest gets back when a booking is cancelled. The rules are in
 * topics/02-writing-a-spec/SPEC.md; StandardRefundPolicy was generated from that specification.
 */
public interface RefundPolicy {

    /** The amount refunded to the guest, in rupees with 2 decimals. */
    BigDecimal refund(Booking booking, LocalDate cancelledOn, CancelledBy cancelledBy);
}
