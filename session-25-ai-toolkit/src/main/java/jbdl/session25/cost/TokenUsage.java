package jbdl.session25.cost;

/**
 * The tokens one request (or one whole agent run) used, split the way the bill splits them.
 * AI tools report these numbers: Claude Code with {@code --output-format json}, the APIs in every
 * response ({@code usage}).
 *
 * @param input      normal input tokens
 * @param cacheWrite input tokens written to the prompt cache
 * @param cacheRead  input tokens read from the prompt cache
 * @param output     tokens the model wrote (its answer, its tool calls and its thinking)
 */
public record TokenUsage(long input, long cacheWrite, long cacheRead, long output) {

    /** All the input the model read, cached or not. */
    public long allInput() {
        return input + cacheWrite + cacheRead;
    }
}
