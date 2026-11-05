package jbdl.splitwise.expenses.split;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everyone pays the same. The tricky part is the paise that don't divide evenly: 100.00 among 3 is
 * 33.333... Rounding everyone down gives 99.99 (one paisa lost); rounding up gives 100.02. Instead:
 * give everyone the rounded-down share, then hand out the leftover paise one by one from the first
 * person: 33.34 + 33.33 + 33.33 = 100.00 exactly.
 */
public class EqualSplit implements SplitStrategy {

    private static final BigDecimal ONE_PAISA = new BigDecimal("0.01");

    @Override
    public Map<Long, BigDecimal> split(BigDecimal amount, List<Long> participants, Map<Long, BigDecimal> values) {
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("an expense needs at least one participant");
        }
        BigDecimal count = BigDecimal.valueOf(participants.size());
        BigDecimal baseShare = amount.divide(count, 2, RoundingMode.DOWN);        // 33.33
        BigDecimal leftover = amount.subtract(baseShare.multiply(count));          // 0.01
        int extraPaise = leftover.divide(ONE_PAISA).intValue();                    // 1

        Map<Long, BigDecimal> shares = new LinkedHashMap<>(); // keeps the participants' order
        for (int i = 0; i < participants.size(); i++) {
            BigDecimal share = baseShare;
            if (i < extraPaise) {
                share = share.add(ONE_PAISA); // the first `extraPaise` people pay one paisa more
            }
            shares.put(participants.get(i), share);
        }
        return shares;
    }
}
