package jbdl.session20.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * config-server: one place for the settings of all the other services. A service asks it at start-up
 * with its name, e.g. GET /order-service/default, and gets the matching file from config-repo.
 *
 * Start it first:
 *   java -jar target/session-20-messaging-resilience-1.0.0-SNAPSHOT-config.jar
 */
@SpringBootApplication
// Turns this application into a config server (the /{name}/{profile} endpoints)
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        // "config-server" loads application-config-server.properties; "native" makes the server read
        // plain files (config-repo) instead of a Git repository.
        new SpringApplicationBuilder(ConfigServerApplication.class).profiles("config-server", "native").run(args);
    }
}
