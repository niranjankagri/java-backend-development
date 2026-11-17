package jbdl.session25.cost;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Topic 3: estimating tokens, and measuring a project folder before giving it to an AI tool. */
class CodebaseSizeTest {

    @Test
    void aboutFourCharactersAreOneToken() {
        assertThat(TokenEstimator.estimate(0)).isZero();
        assertThat(TokenEstimator.estimate(1)).isEqualTo(1);
        assertThat(TokenEstimator.estimate(4)).isEqualTo(1);
        assertThat(TokenEstimator.estimate(5)).isEqualTo(2);
        assertThat(TokenEstimator.estimate(400_000)).isEqualTo(100_000);
    }

    @Test
    void countsSourceFilesButNotBuildOutputOrOtherFiles(@TempDir Path project) throws IOException {
        // @TempDir: JUnit creates an empty folder for this test and deletes it afterwards
        Files.createDirectories(project.resolve("src"));
        Files.createDirectories(project.resolve("target/classes"));
        Files.writeString(project.resolve("src/App.java"), "class App {}");              // 12 characters
        Files.writeString(project.resolve("README.md"), "# App");                        // 5 characters
        Files.writeString(project.resolve("target/classes/App.java"), "copied by the build");
        Files.writeString(project.resolve("logo.png"), "not source");

        CodebaseSize.Size size = CodebaseSize.measure(project);

        assertThat(size.files()).isEqualTo(2);
        assertThat(size.characters()).isEqualTo(17);
    }
}
