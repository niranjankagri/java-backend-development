package jbdl.session28.rag;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Measures the retrieval step alone (topic 5): for each question we know a short text (the
 * "needle") that only a chunk with the answer contains. A question is a hit if one of the top
 * {@link CourseAssistant#TOP_K} chunks contains its needle. Run it from the repository folder:
 * {@code java -cp session-28-rag/target/classes jbdl.session28.rag.RetrievalEval .}
 */
public final class RetrievalEval {

    /**
     * One test question.
     *
     * @param question what a developer asks
     * @param needle   a text that only a chunk with the answer contains
     */
    public record Case(String question, String needle) {
    }

    /** The questions of topics 4 and 5. Some use the words of the docs, some do not, on purpose. */
    public static final List<Case> CASES = List.of(
            new Case("Which port does the Session 20 order-service run on?", "8200"),
            new Case("Which Docker image does the Kafka end-to-end test start?", "kafka-native"),
            new Case("Why did the Redis test in Session 15 fail sometimes?", "asynchronously"),
            new Case("Which Spring Boot starter replaced spring-boot-starter-aop in Spring Boot 4?",
                    "spring-boot-starter-aspectj"),
            new Case("What refund does a guest get when cancelling 1 day before check-in?", "1 or fewer"),
            new Case("How much money does a guest get back if they cancel the day before arrival?", "1 or fewer"),
            new Case("How does the Airbnb backend stop two guests from booking the same nights?", "PESSIMISTIC_WRITE"),
            new Case("What does the smoke test check after a deployment?", "must be 409"),
            new Case("On which Java versions does CI run the tests?", "17 and 25"));

    private RetrievalEval() {
    }

    /** Prints hit or miss per question, and the total, for plain words and for stemmed words. */
    public static void main(String[] args) throws IOException {
        List<Chunk> chunks = CourseAssistant.loadChunks(Path.of(args.length > 0 ? args[0] : "."));
        for (boolean stemming : new boolean[] {false, true}) {
            VectorIndex index = VectorIndex.of(chunks, stemming);
            int hits = 0;
            System.out.println(stemming ? "With stemming:" : "Plain words:");
            for (Case c : CASES) {
                boolean hit = false;
                for (VectorIndex.Hit found : index.search(c.question(), CourseAssistant.TOP_K)) {
                    if (found.chunk().text().contains(c.needle())) {
                        hit = true;
                    }
                }
                if (hit) {
                    hits++;
                }
                System.out.println("  " + (hit ? "hit " : "MISS") + "  " + c.question());
            }
            System.out.println("  " + hits + " of " + CASES.size() + " questions found the chunk with the answer");
        }
    }
}
