package jbdl.splitwise.expenses.split;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Each person's amount is given; they must add up to the total, or the expense is refused. */
public class ExactSplit implements SplitStrategy {

    @Override
    public Map<Long, BigDecimal> split(BigDecimal amount, List<Long> participants, Map<Long, BigDecimal> values) {
        Map<Long, BigDecimal> shares = new LinkedHashMap<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (Long userId : participants) {
            BigDecimal share = values.get(userId);
            if (share == null || share.signum() < 0) {
                throw new IllegalArgumentException("exact split needs a non-negative amount for user " + userId);
            }
            shares.put(userId, share);
            sum = sum.add(share);
        }
        // compareTo, not equals: for BigDecimal, 900.0 and 900.00 are NOT equals() (different scale)
        if (sum.compareTo(amount) != 0) {
            throw new IllegalArgumentException("exact amounts add up to " + sum + ", not " + amount);
        }
        return shares;
    }
}
