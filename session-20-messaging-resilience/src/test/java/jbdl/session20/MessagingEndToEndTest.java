package jbdl.session20;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import jbdl.session20.config.ConfigServerApplication;
import jbdl.session20.notifications.NotificationServiceApplication;
import jbdl.session20.orders.OrderEvents;
import jbdl.session20.orders.OrderPlaced;
import jbdl.session20.orders.OrderServiceApplication;
import jbdl.session20.shipping.ShippingServiceApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The whole session at once, all real: a Kafka broker in Docker (Testcontainers) and the four services
 * on free ports: config-server, shipping-service, order-service and notification-service.
 * Without Docker the class is skipped, not failed (CI has Docker).
 */
@Testcontainers(disabledWithoutDocker = true)
class MessagingEndToEndTest {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.1"));

    private final List<ConfigurableApplicationContext> running = new ArrayList<>();
    private ConfigurableApplicationContext shipping;
    private ConfigurableApplicationContext orders;
    private RestClient orderService;
    private RestClient notificationService;

    @BeforeEach
    void startServices() {
        ConfigurableApplicationContext config = start(new SpringApplicationBuilder(ConfigServerApplication.class)
                .profiles("config-server", "native"), "--server.port=0");
        String configImport = "--spring.config.import=configserver:http://localhost:" + port(config);
        String kafka = "--spring.kafka.bootstrap-servers=" + KAFKA.getBootstrapServers();

        shipping = start(new SpringApplicationBuilder(ShippingServiceApplication.class).profiles("shipping"),
                "--server.port=0");
        orders = start(OrderServiceApplication.builder(),
                "--server.port=0", configImport, kafka, "--shipping.url=http://localhost:" + port(shipping));
        ConfigurableApplicationContext notifications = start(NotificationServiceApplication.builder(),
                "--server.port=0", configImport, kafka);

        orderService = RestClient.create("http://localhost:" + port(orders));
        notificationService = RestClient.create("http://localhost:" + port(notifications));
    }

    @AfterEach
    void stopServices() {
        for (ConfigurableApplicationContext context : running) {
            context.close();
        }
    }

    @Test
    @DisplayName("An order: fee from shipping-service, OrderPlaced over Kafka, message in the config-server template")
    void orderToNotification() {
        Map<String, Object> order = placeOrder("asha@example.com", "Delhi", "450");

        // 450 is below config-server's free-shipping amount (500), so shipping-service's fee for Delhi applies
        assertThat(order.get("shippingFee")).isEqualTo(40);
        assertThat(order.get("shippingFeeSource")).isEqualTo("shipping-service");
        assertThat(order.get("total")).isEqualTo(490);

        // asynchronous: the order is answered before the message exists, so wait for it to arrive
        waitUntil(() -> !notificationsTo("asha@example.com").isEmpty());
        assertThat(notificationsTo("asha@example.com")).singleElement().satisfies(notification ->
                assertThat(notification.get("message"))
                        .isEqualTo("Hello asha@example.com! Thank you for order " + order.get("id") + ". Total: 490 INR."));
    }

    @Test
    @DisplayName("The same event sent twice: notification-service writes one message")
    void duplicateEvent() {
        OrderEvents events = orders.getBean(OrderEvents.class);
        OrderPlaced event = new OrderPlaced("event-dup", 100, "ravi@example.com", new BigDecimal("700"));
        OrderPlaced marker = new OrderPlaced("event-marker", 100, "ravi@example.com", new BigDecimal("700"));

        events.publish(event);
        events.publish(event);
        // same order id = same key = same partition: the marker is read after both copies
        events.publish(marker);

        waitUntil(() -> eventIdsTo("ravi@example.com").contains("event-marker"));
        assertThat(eventIdsTo("ravi@example.com")).containsExactly("event-dup", "event-marker");
    }

    @Test
    @DisplayName("shipping-service stopped: the order still goes through, with config-server's fallback fee")
    void shippingServiceDown() {
        shipping.close();

        Map<String, Object> order = placeOrder("meera@example.com", "Delhi", "450");

        assertThat(order.get("shippingFee")).isEqualTo(99);
        assertThat(order.get("shippingFeeSource")).isEqualTo("fallback: shipping-service failed");
    }

    private Map<String, Object> placeOrder(String email, String city, String amount) {
        return orderService.post().uri("/orders").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("customerEmail", email, "city", city, "amount", new BigDecimal(amount)))
                .retrieve().body(new ParameterizedTypeReference<Map<String, Object>>() { });
    }

    /**
     * The messages to one customer. The Kafka broker is shared by the tests of this class, so each test
     * uses its own customer and looks only at that customer's messages.
     */
    private List<Map<String, Object>> notificationsTo(String email) {
        List<Map<String, Object>> all = notificationService.get().uri("/notifications").retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() { });
        List<Map<String, Object>> toCustomer = new ArrayList<>();
        for (Map<String, Object> notification : all) {
            if (email.equals(notification.get("to"))) {
                toCustomer.add(notification);
            }
        }
        return toCustomer;
    }

    private List<Object> eventIdsTo(String email) {
        List<Object> ids = new ArrayList<>();
        for (Map<String, Object> notification : notificationsTo(email)) {
            ids.add(notification.get("eventId"));
        }
        return ids;
    }

    /** Checks the condition every 100 ms, for up to 20 s (the first events wait for the consumer to join). */
    private static void waitUntil(BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("condition not met within 20 s");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted", e);
            }
        }
    }

    private ConfigurableApplicationContext start(SpringApplicationBuilder builder, String... args) {
        ConfigurableApplicationContext context = builder.run(args);
        running.add(context);
        return context;
    }

    private static int port(ConfigurableApplicationContext context) {
        return Integer.parseInt(context.getEnvironment().getProperty("local.server.port"));
    }
}
