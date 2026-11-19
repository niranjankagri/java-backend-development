package jbdl.session27.refund;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The parts of a booking a refund depends on. The checks (no nulls, no negative amounts) are the
 * refund policy's job, as the specification says (rule R1), so this record accepts any values.
 *
 * @param nightsTotal what the guest paid for the nights, after any discount
 * @param cleaningFee what the guest paid for cleaning
 * @param checkIn     the day of arrival (the first night)
 * @param checkOut    the day of leaving
 */
public record Booking(BigDecimal nightsTotal, BigDecimal cleaningFee, LocalDate checkIn, LocalDate checkOut) {
}
