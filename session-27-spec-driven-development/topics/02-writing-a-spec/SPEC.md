# Specification: refund on cancellation

## Goal

When a booking is cancelled, compute how much money the guest gets back. The amount is shown to the
guest before they confirm the cancellation, and the payment provider refunds exactly this amount.

## Interface

Implement the class `StandardRefundPolicy` in package `jbdl.session27.refund`. It implements
`RefundPolicy` and has a public constructor without parameters. These types already exist; do not
write them again:

```java
public record Booking(BigDecimal nightsTotal, BigDecimal cleaningFee, LocalDate checkIn, LocalDate checkOut) {}

public enum CancelledBy { GUEST, HOST }

public interface RefundPolicy {
    /** The amount refunded to the guest, in rupees with 2 decimals. */
    BigDecimal refund(Booking booking, LocalDate cancelledOn, CancelledBy cancelledBy);
}
```

## Definitions

- **days before check-in** = the number of days from `cancelledOn` to `checkIn`
  (`ChronoUnit.DAYS.between(cancelledOn, checkIn)`): 1 on the day before check-in, 0 on the check-in
  day, negative during the stay.

## Rules

| # | Rule |
|---|---|
| R1 | No argument and no field of `booking` may be null; `nightsTotal` and `cleaningFee` must not be negative; `checkOut` must be after `checkIn`. Otherwise throw `IllegalArgumentException`. |
| R2 | A booking cannot be cancelled on or after its `checkOut` day: throw `IllegalArgumentException`. |
| R3 | **Host** cancels before the check-in day (days before check-in >= 1): the refund is `nightsTotal + cleaningFee`. |
| R4 | **Host** cancels on or after the check-in day: throw `IllegalArgumentException` (a started stay is handled by support, not by this policy). |
| R5 | **Guest** cancels: the nights part of the refund depends on the days before check-in: 7 or more: 100 % of `nightsTotal`; 2 to 6: 50 %; 1 or fewer (including during the stay): 0 %. |
| R6 | **Guest** cancels before the check-in day (days before check-in >= 1): the whole `cleaningFee` is refunded too. On or after the check-in day: nothing of it. |
| R7 | Add the parts first, then round once: 2 decimals, `RoundingMode.HALF_UP`. The result always has scale 2 (`0.00`, not `0`). |

## Examples (acceptance criteria)

Booking: `nightsTotal` 10000.00, `cleaningFee` 500.00, `checkIn` 2026-12-20, `checkOut` 2026-12-23.

| Cancelled on | By | Days before | Refund | Rules |
|---|---|---|---|---|
| 2026-12-01 | GUEST | 19 | 10500.00 | R5, R6 |
| 2026-12-15 | GUEST | 5 | 5500.00 | R5, R6 |
| 2026-12-19 | GUEST | 1 | 500.00 | R5, R6 |
| 2026-12-21 | GUEST | -1 | 0.00 | R5, R6 |
| 2026-12-19 | HOST | 1 | 10500.00 | R3 |
| 2026-12-20 | HOST | 0 | `IllegalArgumentException` | R4 |

## Not in scope

- Partial refunds for nights not yet stayed when a guest leaves early.
- Currencies other than rupees; taxes; the payment itself.

## Answer format

Only one Java code block with the complete class `StandardRefundPolicy`, no explanation. Java 17, only
the JDK.
