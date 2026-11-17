package jbdl.session25.cost;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Turns token counts into money: for each kind of token, tokens / 1,000,000 x price per million.
 */
public final class CostCalculator {

    private static final BigDecimal MILLION = new BigDecimal("1000000");

    private CostCalculator() {
    }

    /** The cost of {@code usage} on {@code model}, in US dollars with 4 decimals. */
    public static BigDecimal cost(ModelPrice model, TokenUsage usage) {
        BigDecimal total = BigDecimal.ZERO;
        total = total.add(part(usage.input(), model.inputPerMillion()));
        total = total.add(part(usage.cacheWrite(), model.cacheWritePerMillion()));
        total = total.add(part(usage.cacheRead(), model.cacheReadPerMillion()));
        total = total.add(part(usage.output(), model.outputPerMillion()));
        return total.setScale(4, RoundingMode.HALF_UP);
    }

    /** True if a request with this much input, plus room for the answer, fits into the model. */
    public static boolean fits(ModelPrice model, long inputTokens, long answerTokens) {
        return inputTokens + answerTokens <= model.contextWindow();
    }

    private static BigDecimal part(long tokens, BigDecimal pricePerMillion) {
        // tokens x price / 1,000,000; dividing by a million is always exact, so no rounding here
        return BigDecimal.valueOf(tokens).multiply(pricePerMillion).divide(MILLION);
    }
}
