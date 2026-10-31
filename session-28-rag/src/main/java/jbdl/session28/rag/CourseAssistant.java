package jbdl.session28.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * A developer assistant for this course: it indexes every Markdown file of the repository and,
 * for a question, prints the best chunks (on stderr) and the prompt for the model (on stdout).
 * The prompt is then sent to a model, in topic 4 with {@code claude -p}:
 *
 * <pre>
 * java -cp session-28-rag/target/classes jbdl.session28.rag.CourseAssistant . "Why did the Redis test fail?" \
 *     | claude -p --model haiku --tools ""
 * </pre>
 */
public final class CourseAssistant {

    /** How many chunks go into the prompt. */
    static final int TOP_K = 4;

    private CourseAssistant() {
    }

    /** Reads and chunks every .md file below {@code root}, skipping build output and hidden folders. */
    public static List<Chunk> loadChunks(Path root) throws IOException {
        List<Chunk> chunks = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted().toList()) {
                String relative = root.relativize(path).toString().replace('\\', '/');
                if (relative.endsWith(".md") && !isSkipped(relative)) {
                    chunks.addAll(MarkdownChunker.chunk(relative, Files.readString(path, StandardCharsets.UTF_8)));
                }
            }
        }
        return chunks;
    }

    /**
     * Build output and hidden folders are not documentation. Session 28's own folder is skipped too:
     * its READMEs discuss the very questions of the experiments, with their answers.
     */
    private static boolean isSkipped(String relative) {
        return relative.startsWith(".") || relative.contains("/.") || relative.contains("target/")
                || relative.startsWith("out/") || relative.startsWith("session-28-");
    }

    /** Arguments: the repository folder and the question. */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: CourseAssistant <repository folder> <question>");
            System.exit(1);
        }
        List<Chunk> chunks = loadChunks(Path.of(args[0]));
        VectorIndex index = VectorIndex.of(chunks);
        List<VectorIndex.Hit> hits = index.search(args[1], TOP_K);

        System.err.printf("%d chunks indexed. Best matches:%n", index.size());
        for (VectorIndex.Hit hit : hits) {
            System.err.printf("  %.3f  %s%n", hit.score(), hit.chunk().reference());
        }
        System.out.print(RagPrompt.build(args[1], hits));
    }
}
