package jbdl.session26.prompt;

import java.util.ArrayList;
import java.util.List;

/**
 * A prompt for a coding task, built from the parts of topic 2: role, context, task, constraints,
 * examples and the answer format. {@link #render()} turns it into the text sent to the model.
 * Every part is optional except the task, and parts that were not set are left out of the text.
 *
 * <pre>{@code
 * String prompt = DeveloperPrompt.task("Write a method that ...")
 *         .role("a senior Java developer")
 *         .constraint("Java 17, no libraries")
 *         .render();
 * }</pre>
 *
 * The prompts of all experiments in this session were rendered by {@link SlugPrompts} with this class.
 */
public final class DeveloperPrompt {

    /**
     * One input and the output the model should give for it (few-shot prompting, topic 3).
     *
     * @param input  what goes in
     * @param output what must come out
     */
    public record Example(String input, String output) {
    }

    private final String task;
    private String role = "";
    private String context = "";
    private final List<String> constraints = new ArrayList<>();
    private final List<Example> examples = new ArrayList<>();
    private String answerFormat = "";

    private DeveloperPrompt(String task) {
        this.task = task;
    }

    /** Starts a prompt with the only part that is required: what the model should do. */
    public static DeveloperPrompt task(String task) {
        if (task == null || task.isBlank()) {
            throw new IllegalArgumentException("A prompt needs a task");
        }
        return new DeveloperPrompt(task);
    }

    /** Who the model should act as, e.g. "a senior Java developer". */
    public DeveloperPrompt role(String role) {
        this.role = role;
        return this; // returning this lets the calls be chained: task(...).role(...).constraint(...)
    }

    /** What the model needs to know about the project. */
    public DeveloperPrompt context(String context) {
        this.context = context;
        return this;
    }

    /** One rule the answer must follow. Call it once per rule. */
    public DeveloperPrompt constraint(String constraint) {
        constraints.add(constraint);
        return this;
    }

    /** One worked example. Call it once per example. */
    public DeveloperPrompt example(String input, String output) {
        examples.add(new Example(input, output));
        return this;
    }

    /** How the answer should look, e.g. "only one Java code block". */
    public DeveloperPrompt answerFormat(String answerFormat) {
        this.answerFormat = answerFormat;
        return this;
    }

    /** The prompt as text, in the order role, context, task, constraints, examples, answer format. */
    public String render() {
        StringBuilder text = new StringBuilder();
        if (!role.isBlank()) {
            text.append("You are ").append(role).append(".\n\n");
        }
        if (!context.isBlank()) {
            text.append("Context:\n").append(context).append("\n\n");
        }
        text.append("Task:\n").append(task).append("\n\n");
        if (!constraints.isEmpty()) {
            text.append("Constraints:\n");
            for (String constraint : constraints) {
                text.append("- ").append(constraint).append('\n');
            }
            text.append('\n');
        }
        if (!examples.isEmpty()) {
            text.append("Examples:\n");
            for (Example example : examples) {
                text.append("Input: ").append(example.input()).append('\n');
                text.append("Output: ").append(example.output()).append('\n');
            }
            text.append('\n');
        }
        if (!answerFormat.isBlank()) {
            text.append("Answer format:\n").append(answerFormat).append('\n');
        }
        return text.toString().strip() + "\n";
    }
}
