package jbdl.session28.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Topics 2 and 5: searching the index, and what stemming changes. */
class VectorIndexTest {

    private static final List<Chunk> CHUNKS = List.of(
            new Chunk("session-15/README.md", "What the real Redis taught us",
                    "RedisTest failed sometimes: Spring Data Redis writes cache entries asynchronously."),
            new Chunk("session-20/README.md", "Messaging", "The order-service runs on port 8200 and sends events to Kafka."),
            new Chunk("session-27/SPEC.md", "Rules", "A guest who cancels 1 or fewer days before check-in gets 0 % of the nights."));

    @Test
    void theBestMatchComesFirst() {
        List<VectorIndex.Hit> hits = VectorIndex.of(CHUNKS).search("Which port does the order-service use?", 3);

        assertThat(hits.get(0).chunk().source()).isEqualTo("session-20/README.md");
        assertThat(hits.get(0).score()).isGreaterThan(0.0);
    }

    @Test
    void chunksWithoutAnyCommonWordAreNotReturned() {
        List<VectorIndex.Hit> hits = VectorIndex.of(CHUNKS).search("Which port does the order-service use?", 3);

        // the Redis and refund chunks share no word with the question
        assertThat(hits).hasSize(1);
    }

    @Test
    void stemmingFindsFailedForFail() {
        String question = "Why did the test fail?";

        List<VectorIndex.Hit> plain = VectorIndex.of(CHUNKS, false).search(question, 3);
        List<VectorIndex.Hit> stemmed = VectorIndex.of(CHUNKS, true).search(question, 3);

        // "test" matches "RedisTest"? No: "redistest" is one word. Only "fail" = "failed" can match.
        assertThat(plain).isEmpty();
        assertThat(stemmed).extracting(hit -> hit.chunk().source()).containsExactly("session-15/README.md");
    }

    @Test
    void theFileAndHeadingAreSearchedToo() {
        // "redis" is in the heading of the first chunk, "spec" in the file name of the third
        assertThat(VectorIndex.of(CHUNKS).search("spec", 1).get(0).chunk().source()).isEqualTo("session-27/SPEC.md");
    }
}
