package jbdl.session28.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Topic 2: turning text into vectors and comparing them, including what TF-IDF cannot do. */
class TextVectorizerTest {

    private static final List<String> TEXTS = List.of(
            "The guest gets a refund of the cleaning fee.",
            "The Redis cache writes entries asynchronously.",
            "Kafka sends the order event to the notification service.");

    @Test
    void stopWordsAndSingleLettersAreLeftOut() {
        assertThat(TextVectorizer.words("What is the port of a service?", false))
                .containsExactly("port", "service");
    }

    @Test
    void theStemmerCutsCommonEndings() {
        assertThat(TextVectorizer.stem("failed")).isEqualTo("fail");
        assertThat(TextVectorizer.stem("failing")).isEqualTo("fail");
        assertThat(TextVectorizer.stem("nights")).isEqualTo("night");
        assertThat(TextVectorizer.stem("bed")).isEqualTo("bed"); // too short to cut
    }

    @Test
    void aTextIsMostSimilarToItself() {
        TextVectorizer vectorizer = TextVectorizer.fit(TEXTS, true);
        Map<String, Double> refund = vectorizer.vector(TEXTS.get(0));

        assertThat(TextVectorizer.cosine(refund, refund)).isCloseTo(1.0, within(1e-9));
        assertThat(TextVectorizer.cosine(refund, vectorizer.vector(TEXTS.get(1)))).isLessThan(0.2);
    }

    @Test
    void sameMeaningButNoCommonWordGivesZero() {
        // the limit of word-based vectors: a person sees "money back" = "refund", TF-IDF sees no overlap.
        // An embedding model (topic 2) puts these two close together.
        TextVectorizer vectorizer = TextVectorizer.fit(TEXTS, true);

        double similarity = TextVectorizer.cosine(vectorizer.vector("money back"), vectorizer.vector("refund"));

        assertThat(similarity).isZero();
    }
}
