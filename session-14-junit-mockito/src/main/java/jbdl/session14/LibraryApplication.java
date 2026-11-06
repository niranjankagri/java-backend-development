package jbdl.session14;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * A small library: members borrow and return books. It is deliberately built from parts that are
 * easy to replace in tests: a repository, a notifier and a clock are all injected, never created inside
 * the service with `new` or read from a static method like LocalDate.now().
 */
@SpringBootApplication
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }

    /** The real clock in the application; tests pass a fixed one so "today" never changes. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
