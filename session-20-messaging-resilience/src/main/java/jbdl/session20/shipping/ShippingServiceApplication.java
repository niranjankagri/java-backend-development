package jbdl.session20.shipping;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * shipping-service: knows the shipping fee per city. order-service depends on it, so it is the
 * service we stop (Ctrl+C) to watch the circuit breaker and the fallback fee at work (topic 4).
 *
 *   java -jar target/session-20-messaging-resilience-1.0.0-SNAPSHOT-shipping.jar
 */
@SpringBootApplication
public class ShippingServiceApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ShippingServiceApplication.class).profiles("shipping").run(args);
    }
}
