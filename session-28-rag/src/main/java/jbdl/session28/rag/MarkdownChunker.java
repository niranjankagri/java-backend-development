package jbdl.session28.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a Markdown document into chunks, one per heading. A heading is a line that starts with
 * "#", "##" or "###". The text before the first heading becomes a chunk with an empty heading.
 *
 * <p>Why chunks and not whole files? A question is usually answered by one section. Giving the model
 * only the matching sections keeps the prompt small (cheaper, Session 25) and on topic.
 */
public final class MarkdownChunker {

    /** Chunks longer than this are split again at blank lines, so no chunk floods the prompt. */
    static final int MAX_CHARACTERS = 2000;

    private MarkdownChunker() {
    }

    /** The chunks of {@code markdown}, which was read from {@code source}. */
    public static List<Chunk> chunk(String source, String markdown) {
        List<Chunk> chunks = new ArrayList<>();
        String heading = "";
        StringBuilder text = new StringBuilder();
        boolean inCode = false;
        for (String line : markdown.replace("\r\n", "\n").split("\n")) {
            if (line.startsWith("```")) {
                inCode = !inCode; // a "# comment" inside a code block is not a heading
            }
            if (!inCode && isHeading(line)) {
                addChunks(chunks, source, heading, text.toString());
                heading = line.replaceFirst("^#+\\s*", "").strip();
                text.setLength(0);
            } else {
                text.append(line).append('\n');
            }
        }
        addChunks(chunks, source, heading, text.toString());
        return chunks;
    }

    private static boolean isHeading(String line) {
        return line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### ");
    }

    /** Adds the text as one chunk, or as several if it is too long. Empty text adds nothing. */
    private static void addChunks(List<Chunk> chunks, String source, String heading, String text) {
        String rest = text.strip();
        while (rest.length() > MAX_CHARACTERS) {
            // cut at the last blank line before the limit; if there is none, cut at the limit
            int cut = rest.lastIndexOf("\n\n", MAX_CHARACTERS);
            if (cut <= 0) {
                cut = MAX_CHARACTERS;
            }
            chunks.add(new Chunk(source, heading, rest.substring(0, cut).strip()));
            rest = rest.substring(cut).strip();
        }
        if (!rest.isEmpty()) {
            chunks.add(new Chunk(source, heading, rest));
        }
    }
}
