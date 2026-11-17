package jbdl.session25.cost;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Topic 3: what a model call costs. The first test uses real numbers: the token usage that Claude
 * Code reported for its topic 6 run, and the cost it reported for it ($0.2286).
 */
class CostCalculatorTest {

    @Test
    void reproducesTheCostClaudeCodeReportedForItsRun() {
        // From claude -p ... --output-format json (topic 6): 12 normal input tokens, 16,488 written to
        // the 1 hour cache, 159,763 read from the cache, 3,236 output tokens; reported $0.2286246
        TokenUsage run = new TokenUsage(12, 16_488, 159_763, 3_236);

        BigDecimal cost = CostCalculator.cost(ModelPrice.CLAUDE_OPUS_5_5, run);

        // 0.000048 + 0.131904 + 0.0319526 + 0.06472 = 0.2286246
        assertThat(cost).isEqualTo(new BigDecimal("0.2286"));
    }

    @Test
    void theCacheIsWhatMadeTheRunCheap() {
        TokenUsage run = new TokenUsage(12, 16_488, 159_763, 3_236);
        // the same tokens, all as normal input: an agent rereads the conversation on every step
        TokenUsage withoutCache = new TokenUsage(run.allInput(), 0, 0, run.output());

        BigDecimal cost = CostCalculator.cost(ModelPrice.CLAUDE_OPUS_5_5, withoutCache);

        // 176,263 x $4 / million + 3,236 x $20 / million = 0.705052 + 0.06472
        assertThat(cost).isEqualTo(new BigDecimal("0.7698"));
    }

    @Test
    void outputTokensCostFiveTimesAsMuchAsInputTokens() {
        BigDecimal million = CostCalculator.cost(ModelPrice.CLAUDE_SONNET_5_5, new TokenUsage(1_000_000, 0, 0, 0));
        BigDecimal millionOut = CostCalculator.cost(ModelPrice.CLAUDE_SONNET_5_5, new TokenUsage(0, 0, 0, 1_000_000));

        assertThat(million).isEqualTo(new BigDecimal("2.0000"));
        assertThat(millionOut).isEqualTo(new BigDecimal("10.0000"));
    }

    @Test
    void aRequestMustLeaveRoomForTheAnswer() {
        assertThat(CostCalculator.fits(ModelPrice.CLAUDE_HAIKU_5_5, 900_000, 16_000)).isTrue();
        // the window holds input AND output: 990,000 + 16,000 is more than 1,000,000
        assertThat(CostCalculator.fits(ModelPrice.CLAUDE_HAIKU_5_5, 990_000, 16_000)).isFalse();
    }
}
