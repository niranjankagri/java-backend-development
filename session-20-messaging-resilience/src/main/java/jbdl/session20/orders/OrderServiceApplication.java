package jbdl.session20.orders;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;

/**
 * order-service: takes orders, asks shipping-service for the fee, and tells everyone who is interested
 * that an order was placed, by publishing an {@link OrderPlaced} event to Kafka. It does not know who
 * reads the event (today: notification-service).
 *
 * Start config-server, Kafka and shipping-service first, then:
 *   java -jar target/session-20-messaging-resilience-1.0.0-SNAPSHOT-orders.jar
 */
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        builder().run(args);
    }

    /**
     * How this service is started, by main() and by the tests: the "orders" profile loads
     * application-orders.properties (port, config-server, Kafka, shipping-service).
     *
     * Two settings are set here and not in the properties file, because the config client needs them
     * BEFORE it reads application-orders.properties (where they would come too late):
     * <ul>
     *   <li>spring.application.name: the name the client asks config-server for (/order-service/...)</li>
     *   <li>spring.cloud.config.enabled=true: only needed because config-server lives in the same
     *       module, and its library switches the config client off in every application on the classpath</li>
     * </ul>
     */
    public static SpringApplicationBuilder builder() {
        return new SpringApplicationBuilder(OrderServiceApplication.class)
                .profiles("orders")
                .properties("spring.application.name=order-service", "spring.cloud.config.enabled=true");
    }

    /**
     * The topic the events go to. At start-up Spring Kafka creates it if it does not exist yet:
     * 3 partitions (up to 3 consumers of one group can read in parallel), 1 copy (one broker here;
     * production clusters keep 3 copies on different brokers).
     */
    @Bean
    NewTopic ordersPlacedTopic() {
        return TopicBuilder.name(KafkaOrderEvents.TOPIC).partitions(3).replicas(1).build();
    }
}
