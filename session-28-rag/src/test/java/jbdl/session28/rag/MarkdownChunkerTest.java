package jbdl.session28.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Topic 3: splitting documents into chunks, one per heading. */
class MarkdownChunkerTest {

    @Test
    void oneChunkPerHeadingWithTheTextBelowIt() {
        String markdown = """
                # Session 15
                Caching with Redis.

                ## Start Redis
                Run it in Docker.

                ### What the real Redis taught us
                Writes were asynchronous.
                """;

        List<Chunk> chunks = MarkdownChunker.chunk("session-15/README.md", markdown);

        assertThat(chunks).extracting(Chunk::heading)
                .containsExactly("Session 15", "Start Redis", "What the real Redis taught us");
        assertThat(chunks.get(2).text()).isEqualTo("Writes were asynchronous.");
        assertThat(chunks.get(2).reference()).isEqualTo("session-15/README.md > What the real Redis taught us");
    }

    @Test
    void aHashInsideACodeBlockIsNotAHeading() {
        String markdown = """
                ## Run it
                ```bash
                # start the app
                ./mvnw spring-boot:run
                ```
                """;

        List<Chunk> chunks = MarkdownChunker.chunk("README.md", markdown);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).contains("# start the app");
    }

    @Test
    void textBeforeTheFirstHeadingIsKeptAndEmptySectionsAreDropped() {
        List<Chunk> chunks = MarkdownChunker.chunk("PROMPT.md", "Write the class.\n\n## Empty\n\n## Rules\nR1");

        assertThat(chunks).extracting(Chunk::heading).containsExactly("", "Rules");
        assertThat(chunks.get(0).reference()).isEqualTo("PROMPT.md"); // no heading: only the file
    }

    @Test
    void aLongSectionIsSplitAtBlankLines() {
        // 30 paragraphs of 100 characters: about 3,000 characters under one heading
        StringBuilder markdown = new StringBuilder("## Long\n");
        for (int i = 0; i < 30; i++) {
            markdown.append("x".repeat(100)).append("\n\n");
        }

        List<Chunk> chunks = MarkdownChunker.chunk("long.md", markdown.toString());

        assertThat(chunks).hasSizeGreaterThan(1);
        for (Chunk chunk : chunks) {
            assertThat(chunk.heading()).isEqualTo("Long"); // every part keeps its heading
            assertThat(chunk.text().length()).isLessThanOrEqualTo(MarkdownChunker.MAX_CHARACTERS);
        }
    }
}
