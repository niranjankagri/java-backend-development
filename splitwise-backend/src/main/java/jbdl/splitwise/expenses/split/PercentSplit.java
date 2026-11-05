package jbdl.splitwise.expenses.split;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Each person pays a percentage; the percentages must add up to 100. Rounding each share can leave a
 * paisa over or short (e.g. 33.33 % of 100.00 three times = 99.99), so the LAST person's share is
 * "whatever is left", which makes the total exact.
 */
public class PercentSplit implements SplitStrategy {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Override
    public Map<Long, BigDecimal> split(BigDecimal amount, List<Long> participants, Map<Long, BigDecimal> values) {
        BigDecimal totalPercent = BigDecimal.ZERO;
        for (Long userId : participants) {
            BigDecimal percent = values.get(userId);
            if (percent == null || percent.signum() < 0) {
                throw new IllegalArgumentException("percent split needs a non-negative percentage for user " + userId);
            }
            totalPercent = totalPercent.add(percent);
        }
        if (totalPercent.compareTo(HUNDRED) != 0) {
            throw new IllegalArgumentException("percentages add up to " + totalPercent + ", not 100");
        }

        Map<Long, BigDecimal> shares = new LinkedHashMap<>();
        BigDecimal assigned = BigDecimal.ZERO;
        for (int i = 0; i < participants.size(); i++) {
            Long userId = participants.get(i);
            BigDecimal share;
            if (i == participants.size() - 1) {
                share = amount.subtract(assigned); // the last person takes the rest: no paisa lost
            } else {
                share = amount.multiply(values.get(userId)).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            }
            shares.put(userId, share);
            assigned = assigned.add(share);
        }
        return shares;
    }
}
