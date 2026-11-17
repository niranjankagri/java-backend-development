package jbdl.session25.compare;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The problem every AI tool had to solve in topic 6: the price of a stay.
 * The rules are in topics/06-same-problem-many-tools/PROBLEM.md; the same tests check every solution.
 */
public interface PriceCalculator {

    /**
     * Total price of a stay from {@code checkIn} (first night) to {@code checkOut} (day of leaving),
     * in rupees with 2 decimals.
     */
    BigDecimal total(BigDecimal nightlyPrice, LocalDate checkIn, LocalDate checkOut);
}
