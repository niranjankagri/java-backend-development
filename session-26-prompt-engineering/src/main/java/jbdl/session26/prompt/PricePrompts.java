package jbdl.session26.prompt;

/**
 * The two prompts of the chain-of-thought experiment (topic 4). Both ask for the price of the same
 * stay, with the rules of Session 25's PROBLEM.md. The first wants only the number; the second asks
 * the model to write down every step before the number.
 *
 * <p>The right answer: 8 nights from Thursday 24 Dec 2026, 2 of them Friday or Saturday nights.
 * (6 x 1234.50 + 2 x 1481.40) x 0.9 + 500 = 9832.82.
 */
public final class PricePrompts {

    /** The question both prompts ask. */
    static final String QUESTION = "A stay costs 1234.50 per night. A Friday night or a Saturday night costs"
            + " 20 % more. A stay of 7 nights or more gets 10 % off the sum of all its nights. A cleaning"
            + " fee of 500.00 is added after the discount. Round only the final total to 2 decimals."
            + " Check-in is Thursday 24 December 2026, check-out is Friday 1 January 2027."
            + " What is the total price?";

    private PricePrompts() {
    }

    /** Direct: the answer only. */
    public static String direct() {
        return DeveloperPrompt.task(QUESTION)
                .answerFormat("Only the total as a number with 2 decimals, nothing else.")
                .render();
    }

    /** Chain of thought: the steps first, then the answer on a line of its own. */
    public static String stepByStep() {
        return DeveloperPrompt.task(QUESTION)
                .answerFormat("Work it out step by step: list every night with its weekday and price,"
                        + " then the sum, the discount and the fee. End with one line: TOTAL: <number>")
                .render();
    }

    /** Prints the direct prompt, or the step-by-step prompt with the argument "steps". */
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("steps")) {
            System.out.print(stepByStep());
        } else {
            System.out.print(direct());
        }
    }
}
