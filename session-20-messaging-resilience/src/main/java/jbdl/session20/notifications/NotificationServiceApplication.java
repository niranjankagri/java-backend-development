package jbdl.session20.notifications;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * notification-service: reads OrderPlaced events from Kafka and writes the customer a message.
 * order-service does not call it and does not even know it exists; if this service is down, orders
 * still work, and the events wait in Kafka until it is back.
 *
 *   java -jar target/session-20-messaging-resilience-1.0.0-SNAPSHOT-notifications.jar
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        builder().run(args);
    }

    /**
     * How this service is started, by main() and by the tests: the "notifications" profile loads
     * application-notifications.properties (port, config-server, Kafka).
     *
     * Two settings are set here and not in the properties file, because the config client needs them
     * BEFORE it reads application-notifications.properties (where they would come too late):
     * <ul>
     *   <li>spring.application.name: the name the client asks config-server for (/notification-service/...)</li>
     *   <li>spring.cloud.config.enabled=true: only needed because config-server lives in the same
     *       module, and its library switches the config client off in every application on the classpath</li>
     * </ul>
     */
    public static SpringApplicationBuilder builder() {
        return new SpringApplicationBuilder(NotificationServiceApplication.class)
                .profiles("notifications")
                .properties("spring.application.name=notification-service", "spring.cloud.config.enabled=true");
    }
}
