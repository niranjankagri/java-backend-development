package jbdl.session26.prompt;

/**
 * The three prompts of the hallucination experiment (topic 5). The question is about this course's
 * own code, which the model cannot know: Session 20 was not even on GitHub when it was asked.
 *
 * <p>The true answers at the time of the experiment: port 8200 (application-orders.properties) and the
 * image apache/kafka-native:4.1.1 (MessagingEndToEndTest). The prompts are kept as they were asked; since
 * 23 Nov 2026 the test starts the JVM image apache/kafka:4.1.1 instead (see the comment there).
 */
public final class HallucinationPrompts {

    static final String QUESTION = "In the GitHub repository niranjankagri/java-backend-development, Session 20"
            + " has an order-service. On which port does it run, and which Docker image does its Kafka"
            + " end-to-end test start?";

    private HallucinationPrompts() {
    }

    /** 1: the plain question. */
    public static String plain() {
        return DeveloperPrompt.task(QUESTION).render();
    }

    /** 2: the same question, with permission to say "I don't know". */
    public static String allowUnknown() {
        return DeveloperPrompt.task(QUESTION)
                .constraint("If you cannot know something, answer \"I don't know\" for it instead of guessing")
                .render();
    }

    /** 3: the same question, with the two lines that answer it given as context. */
    public static String withContext() {
        // """ starts a text block: one String over several lines (Java 15+). The simpler version is
        // "line 1\n" + "line 2\n" + ... with \n for every line break.
        return DeveloperPrompt.task(QUESTION)
                .context("""
                        session-20-messaging-resilience/src/main/resources/application-orders.properties, line 3:
                        server.port=8200

                        session-20-messaging-resilience/src/test/java/jbdl/session20/MessagingEndToEndTest.java, line 40:
                        static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.1"));""")
                .constraint("Answer only from the context; say which file each answer comes from")
                .render();
    }

    /**
     * A question with a false premise: Resilience4j 2.3 has no {@code @CircuitBreakerFallback}
     * (its annotations are Bulkhead, CircuitBreaker, RateLimiter, Retry, TimeLimiter and Timer; a
     * fallback is {@code @CircuitBreaker(name = ..., fallbackMethod = ...)}).
     */
    static final String FAKE_ANNOTATION = "In Resilience4j 2.3, how do I use the @CircuitBreakerFallback"
            + " annotation on a Spring Boot method? Give a short code example.";

    /** A question with a false premise: Spring Boot 4.1 has no such property. */
    static final String FAKE_PROPERTY = "What does the Spring Boot 4.1 property"
            + " spring.kafka.consumer.idempotent-delivery=true do? Answer in 3 sentences.";

    /** 4: a question about something that does not exist, asked as if it did. */
    public static String falsePremise(String question) {
        return DeveloperPrompt.task(question).render();
    }

    /** 5: the same question, with the instruction to check the premise first. */
    public static String falsePremiseChecked(String question) {
        return DeveloperPrompt.task(question)
                .constraint("First check that what I ask about really exists in that version."
                        + " If it does not, or you are not sure, say so and show what does exist instead")
                .render();
    }

    /**
     * 6: prompt 5 plus the versions of the project. Without them, all six answers about the
     * annotation assumed Spring Boot 3 and named spring-boot-starter-aop, a starter Spring Boot 4
     * no longer has (it became spring-boot-starter-aspectj).
     */
    public static String falsePremiseWithVersions(String question) {
        return DeveloperPrompt.task(question)
                .context("The project uses Spring Boot 4.1.2, Resilience4j 2.3.0 and Java 17.")
                .constraint("First check that what I ask about really exists in that version."
                        + " If it does not, or you are not sure, say so and show what does exist instead")
                .constraint("Name dependencies exactly as they are called in these versions")
                .render();
    }

    /** Prints prompt 1, 2, 3, or 4a, 4b, 5a, 5b, 6a (a = the annotation, b = the property). */
    public static void main(String[] args) {
        String which = args.length > 0 ? args[0] : "1";
        if (which.equals("1")) {
            System.out.print(plain());
        } else if (which.equals("2")) {
            System.out.print(allowUnknown());
        } else if (which.equals("3")) {
            System.out.print(withContext());
        } else {
            String question = which.endsWith("a") ? FAKE_ANNOTATION : FAKE_PROPERTY;
            if (which.startsWith("4")) {
                System.out.print(falsePremise(question));
            } else if (which.startsWith("6")) {
                System.out.print(falsePremiseWithVersions(question));
            } else {
                System.out.print(falsePremiseChecked(question));
            }
        }
    }
}
