package jbdl.splitwise.expenses.split;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * How to divide an amount among people: one class per split type (the STRATEGY pattern). The expense
 * service doesn't need if/else for each type; it asks SplitStrategies for the right strategy and calls
 * split(). A new split type = one new class, nothing else changes.
 */
public interface SplitStrategy {

    /**
     * @param amount       the total, e.g. 900.00
     * @param participants who shares it, in order (user ids)
     * @param values       per participant: nothing for EQUAL, the amount for EXACT, the percentage for PERCENT
     * @return the share of each participant (same order); the shares always add up to exactly `amount`
     */
    Map<Long, BigDecimal> split(BigDecimal amount, List<Long> participants, Map<Long, BigDecimal> values);
}
