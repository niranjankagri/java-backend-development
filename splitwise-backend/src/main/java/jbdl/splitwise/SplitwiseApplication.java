package jbdl.splitwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Splitwise-like expense sharing. Layers, from the outside in:
 *   controllers (HTTP + JSON)  ->  services (rules, @Transactional)  ->  repositories (database)
 * Packages are by FEATURE (users, groups, expenses), each holding its own controller, service,
 * repository and entities, so everything about one feature is in one place.
 */
@SpringBootApplication
public class SplitwiseApplication {

    public static void main(String[] args) {
        SpringApplication.run(SplitwiseApplication.class, args);
    }
}
