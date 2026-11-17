package jbdl.session25.cost;

import java.math.BigDecimal;

/**
 * What one model costs and how much it can read at once. Prices are US dollars per million tokens,
 * as the providers publish them. They change: always copy them from the provider's pricing page.
 *
 * @param name                 the model's name, for printing
 * @param inputPerMillion      normal input tokens (the prompt, files, the chat so far)
 * @param outputPerMillion     tokens the model writes (here 5 times the input price)
 * @param cacheWritePerMillion input stored in the prompt cache, so the next request can reuse it
 * @param cacheReadPerMillion  input read back from the prompt cache (much cheaper than normal input)
 * @param contextWindow        the most tokens one request can hold: input and output together
 */
public record ModelPrice(String name,
                         BigDecimal inputPerMillion,
                         BigDecimal outputPerMillion,
                         BigDecimal cacheWritePerMillion,
                         BigDecimal cacheReadPerMillion,
                         long contextWindow) {

    /** Claude Opus 5.5 (Anthropic API prices on 31 Oct 2026; cache write = the 1 hour cache). */
    public static final ModelPrice CLAUDE_OPUS_5_5 = new ModelPrice("claude-opus-5-5",
            new BigDecimal("4.00"), new BigDecimal("20.00"), new BigDecimal("8.00"), new BigDecimal("0.20"),
            1_000_000);

    /** Claude Sonnet 5.5 (Anthropic API prices on 31 Oct 2026; cache write = the 1 hour cache). */
    public static final ModelPrice CLAUDE_SONNET_5_5 = new ModelPrice("claude-sonnet-5-5",
            new BigDecimal("2.00"), new BigDecimal("10.00"), new BigDecimal("4.00"), new BigDecimal("0.20"),
            1_000_000);

    /**
     * Claude Haiku 5.5 (Anthropic API prices on 31 Oct 2026, for prompts up to 100K tokens). The two
     * cache prices are not from the price list: they assume the usual rule (writing to the 1 hour
     * cache = 2 x input, reading = 0.1 x input). Check them on the pricing page before relying on them.
     */
    public static final ModelPrice CLAUDE_HAIKU_5_5 = new ModelPrice("claude-haiku-5-5",
            new BigDecimal("0.10"), new BigDecimal("0.50"), new BigDecimal("0.20"), new BigDecimal("0.01"),
            1_000_000);
}
