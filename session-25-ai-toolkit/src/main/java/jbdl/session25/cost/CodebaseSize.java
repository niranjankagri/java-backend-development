package jbdl.session25.cost;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Answers "can I give the AI my whole project, and what does that cost?" for a folder: it counts
 * the characters of the source files, estimates the tokens, and checks them against each model.
 * The topic 3 README shows how to run it and real output for the Airbnb project.
 */
public final class CodebaseSize {

    /** File endings that count as source: code, SQL, configuration, scripts and docs. */
    private static final List<String> SOURCE_ENDINGS =
            List.of(".java", ".sql", ".properties", ".yaml", ".yml", ".xml", ".md", ".sh");

    private CodebaseSize() {
    }

    /**
     * Number of source files and their characters.
     *
     * @param files      how many source files
     * @param characters all their characters together
     */
    public record Size(int files, long characters) {
    }

    /** Counts the source files under {@code folder}, skipping build output (folders named target). */
    public static Size measure(Path folder) throws IOException {
        int files = 0;
        long characters = 0;
        // Files.walk lists every file and folder below the folder; try-with-resources closes it again
        try (Stream<Path> paths = Files.walk(folder)) {
            for (Path path : paths.toList()) {
                if (Files.isRegularFile(path) && isSource(path) && !isBuildOutput(folder, path)) {
                    files++;
                    characters += Files.readString(path, StandardCharsets.UTF_8).length();
                }
            }
        }
        return new Size(files, characters);
    }

    private static boolean isSource(Path path) {
        String name = path.getFileName().toString();
        for (String ending : SOURCE_ENDINGS) {
            if (name.endsWith(ending)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBuildOutput(Path folder, Path path) {
        for (Path part : folder.relativize(path)) {
            if (part.toString().equals("target")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prints the size of the folder given as the first argument and, for each model, whether it
     * fits and what reading it once costs (normal input, and again from the prompt cache).
     */
    public static void main(String[] args) throws IOException {
        Path folder = Path.of(args.length > 0 ? args[0] : ".");
        Size size = measure(folder);
        long tokens = TokenEstimator.estimate(size.characters());
        System.out.printf("%s: %d source files, %,d characters, about %,d tokens%n",
                folder, size.files(), size.characters(), tokens);

        List<ModelPrice> models = List.of(
                ModelPrice.CLAUDE_OPUS_5_5, ModelPrice.CLAUDE_SONNET_5_5, ModelPrice.CLAUDE_HAIKU_5_5);
        for (ModelPrice model : models) {
            boolean fits = CostCalculator.fits(model, tokens, 16_000); // keep room for a long answer
            BigDecimal firstRead = CostCalculator.cost(model, new TokenUsage(tokens, 0, 0, 0));
            BigDecimal cachedRead = CostCalculator.cost(model, new TokenUsage(0, 0, tokens, 0));
            System.out.printf("%-18s fits: %-3s  read once: $%s  read again from the cache: $%s%n",
                    model.name(), fits ? "yes" : "no", firstRead, cachedRead);
        }
    }
}
