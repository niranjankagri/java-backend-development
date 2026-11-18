package jbdl.session26.prompt;

/**
 * The prompts of the topic 3 and topic 5 experiments, from zero-shot to few-shot. All ask for the
 * same thing: a URL slug for an Airbnb listing title ("Bed &amp; Breakfast" becomes
 * "bed-and-breakfast"). They differ only in how much the model is told.
 *
 * <p>Print one with {@code java -cp target/classes jbdl.session26.prompt.SlugPrompts B}.
 */
public final class SlugPrompts {

    private SlugPrompts() {
    }

    /** A: zero-shot and vague. One sentence, the way people often start. */
    public static String vague() {
        return DeveloperPrompt.task("Write a Java method that turns a listing title into a URL slug.").render();
    }

    /** B: zero-shot, but with the anatomy of topic 2: role, context, task, constraints, format. */
    public static String structured() {
        return structuredStart().render();
    }

    /** C: B plus five examples (few-shot). The examples show rules the constraints do not mention. */
    public static String fewShot() {
        return withExamples(structuredStart()).render();
    }

    /**
     * D: C plus the rules the examples only hinted at (topic 5). Run C1 read the 37-character
     * example as "at most 37 characters"; written down, the rule cannot be guessed wrong.
     */
    public static String fewShotWithRules() {
        DeveloperPrompt prompt = structuredStart()
                .constraint("At most 40 characters. Never cut inside a word, unless the first word alone"
                        + " is longer than 40 characters: then cut it at 40")
                .constraint("\"&\" becomes the word \"and\"; if nothing is left, the slug is \"listing\"");
        return withExamples(prompt).render();
    }

    private static DeveloperPrompt withExamples(DeveloperPrompt prompt) {
        return prompt
                .example("Sunny Room & Garden", "sunny-room-and-garden")
                .example("Hôtel Été", "hotel-ete")
                .example("  Loft -- 2 Beds ", "loft-2-beds")
                .example("@@@", "listing")
                .example("Bright two bedroom apartment near the central railway station",
                        "bright-two-bedroom-apartment-near-the");
    }

    private static DeveloperPrompt structuredStart() {
        return DeveloperPrompt.task("Write the class ListingSlug with one method,"
                        + " public static String of(String title), that turns a listing title into the"
                        + " slug used in the listing's URL.")
                .role("a senior Java developer on an Airbnb-like rental platform")
                .context("Hosts type any title, in any language, with any punctuation."
                        + " The slug goes into URLs like /listings/42/<slug>.")
                .constraint("Java 17, only the JDK, package jbdl.session26.slug")
                .constraint("The slug contains only a-z, 0-9 and single dashes, no dash at the start or end")
                .constraint("Beginner-readable code with a class Javadoc")
                .answerFormat("Only one Java code block with the complete class, no explanation.");
    }

    /** Prints prompt A, B, C or D (the first argument). */
    public static void main(String[] args) {
        String which = args.length > 0 ? args[0] : "A";
        if (which.equals("A")) {
            System.out.print(vague());
        } else if (which.equals("B")) {
            System.out.print(structured());
        } else if (which.equals("C")) {
            System.out.print(fewShot());
        } else {
            System.out.print(fewShotWithRules());
        }
    }
}
