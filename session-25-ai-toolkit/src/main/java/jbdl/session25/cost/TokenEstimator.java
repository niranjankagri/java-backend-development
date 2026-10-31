package jbdl.session25.cost;

/**
 * A rough token count without calling any API. Models do not read characters but tokens (pieces of
 * words). For English text and code a common rule of thumb is about 4 characters per token.
 *
 * <p>It is only an estimate: every model family has its own tokenizer, and code with many symbols
 * needs more tokens than prose. For an exact count, ask the provider (for Claude:
 * {@code POST /v1/messages/count_tokens}).
 */
public final class TokenEstimator {

    private static final int CHARACTERS_PER_TOKEN = 4;

    private TokenEstimator() {
    }

    /** About how many tokens {@code characters} characters of text or code are (rounded up). */
    public static long estimate(long characters) {
        // adding (4 - 1) makes the division round up: 1 to 4 characters are 1 token, 5 are 2
        return (characters + CHARACTERS_PER_TOKEN - 1) / CHARACTERS_PER_TOKEN;
    }
}
