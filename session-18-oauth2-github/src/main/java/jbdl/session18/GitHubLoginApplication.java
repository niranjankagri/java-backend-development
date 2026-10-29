package jbdl.session18;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * A small API where people log in with their GitHub account (OAuth 2) instead of a password stored
 * here. After login the app knows who they are, keeps a member record for them, and can read their
 * public GitHub repositories with the access token GitHub gave it.
 *
 * Run it with your own GitHub OAuth App (see the session README):
 *   GITHUB_CLIENT_ID=... GITHUB_CLIENT_SECRET=... java -jar target/session-18-oauth2-github-1.0.0-SNAPSHOT.jar
 * then open http://localhost:8080/api/me in a browser.
 */
@SpringBootApplication
public class GitHubLoginApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitHubLoginApplication.class, args);
    }

    /** The clock for login times; a bean, so a test could replace it with a fixed one. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
