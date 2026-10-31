package jbdl.session26.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Topic 2: the anatomy of a developer prompt. Also checks that the prompts in the topic folders are
 * exactly the ones this code renders, so the READMEs show what was really sent to the model.
 */
class DeveloperPromptTest {

    @Test
    void rendersThePartsInAFixedOrder() {
        String prompt = DeveloperPrompt.task("Write a method that adds two numbers.")
                .answerFormat("Only code.")        // set first, still printed last
                .example("1, 2", "3")
                .constraint("Java 17")
                .context("A calculator app.")
                .role("a Java developer")
                .render();

        assertThat(prompt).isEqualTo("""
                You are a Java developer.

                Context:
                A calculator app.

                Task:
                Write a method that adds two numbers.

                Constraints:
                - Java 17

                Examples:
                Input: 1, 2
                Output: 3

                Answer format:
                Only code.
                """);
    }

    @Test
    void leavesOutThePartsThatWereNotSet() {
        assertThat(DeveloperPrompt.task("Explain what a slug is.").render())
                .isEqualTo("Task:\nExplain what a slug is.\n");
    }

    @Test
    void aPromptNeedsATask() {
        assertThatThrownBy(() -> DeveloperPrompt.task(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eachEscalationStepAddsToTheOneBefore() {
        // B is A's task made precise plus role, context, constraints; C adds examples; D adds two rules
        assertThat(SlugPrompts.vague()).doesNotContain("Constraints:");
        assertThat(SlugPrompts.structured()).contains("Constraints:").doesNotContain("Examples:");
        assertThat(SlugPrompts.fewShot()).contains("Examples:").doesNotContain("At most 40 characters");
        assertThat(SlugPrompts.fewShotWithRules()).contains("At most 40 characters");
    }

    @Test
    void thePromptFilesAreExactlyWhatTheCodeRenders() throws IOException {
        String slug = "topics/03-zero-shot-to-few-shot/prompts/";
        assertThat(SlugPrompts.vague()).isEqualTo(read(slug + "prompt-A.txt"));
        assertThat(SlugPrompts.structured()).isEqualTo(read(slug + "prompt-B.txt"));
        assertThat(SlugPrompts.fewShot()).isEqualTo(read(slug + "prompt-C.txt"));
        assertThat(SlugPrompts.fewShotWithRules()).isEqualTo(read(slug + "prompt-D.txt"));

        String cot = "topics/04-chain-of-thought/prompts/";
        assertThat(PricePrompts.direct()).isEqualTo(read(cot + "direct.txt"));
        assertThat(PricePrompts.stepByStep()).isEqualTo(read(cot + "steps.txt"));

        String hal = "topics/05-iterate-and-fix-hallucinations/prompts/";
        assertThat(HallucinationPrompts.plain()).isEqualTo(read(hal + "prompt-1.txt"));
        assertThat(HallucinationPrompts.allowUnknown()).isEqualTo(read(hal + "prompt-2.txt"));
        assertThat(HallucinationPrompts.withContext()).isEqualTo(read(hal + "prompt-3.txt"));
        assertThat(HallucinationPrompts.falsePremise(HallucinationPrompts.FAKE_ANNOTATION))
                .isEqualTo(read(hal + "prompt-4a.txt"));
        assertThat(HallucinationPrompts.falsePremise(HallucinationPrompts.FAKE_PROPERTY))
                .isEqualTo(read(hal + "prompt-4b.txt"));
        assertThat(HallucinationPrompts.falsePremiseChecked(HallucinationPrompts.FAKE_ANNOTATION))
                .isEqualTo(read(hal + "prompt-5a.txt"));
        assertThat(HallucinationPrompts.falsePremiseChecked(HallucinationPrompts.FAKE_PROPERTY))
                .isEqualTo(read(hal + "prompt-5b.txt"));
        assertThat(HallucinationPrompts.falsePremiseWithVersions(HallucinationPrompts.FAKE_ANNOTATION))
                .isEqualTo(read(hal + "prompt-6a.txt"));
    }

    /** Reads a prompt file of this module; git may have turned its line ends into \r\n on Windows. */
    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
