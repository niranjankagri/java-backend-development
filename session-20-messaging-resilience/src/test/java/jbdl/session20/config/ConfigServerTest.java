package jbdl.session20.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import jbdl.session20.orders.OrderServiceApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.RestClient;

/**
 * Topic 3 with real applications: config-server on a free port, and order-service started once WITH it
 * and once WITHOUT it. Settings are passed as command-line arguments, as with java -jar.
 *
 * order-service does not need Kafka to start (it connects on the first event), and
 * spring.kafka.admin.auto-create=false stops it from trying to create its topic.
 */
class ConfigServerTest {

    private final List<ConfigurableApplicationContext> running = new ArrayList<>();

    @AfterEach
    void stopAll() {
        for (ConfigurableApplicationContext context : running) {
            context.close();
        }
    }

    @Test
    @DisplayName("config-server serves order-service.properties at /order-service/default")
    void servesSettings() {
        int port = startConfigServer();

        String answer = RestClient.create("http://localhost:" + port).get().uri("/order-service/default")
                .retrieve().body(String.class);

        assertThat(answer)
                .contains("\"name\":\"order-service\"")
                .contains("\"orders.free-shipping-from\":\"500\"")
                .contains("\"orders.fallback-shipping-fee\":\"99\"");
    }

    @Test
    @DisplayName("order-service WITH config-server: the server's values win over its own file (500, not 1000)")
    void clientUsesServerValues() {
        int port = startConfigServer();

        ConfigurableApplicationContext orders = startOrderService("--spring.config.import=configserver:http://localhost:" + port);

        assertThat(orders.getEnvironment().getProperty("orders.free-shipping-from")).isEqualTo("500");
        assertThat(orders.getEnvironment().getProperty("orders.fallback-shipping-fee")).isEqualTo("99");
    }

    @Test
    @DisplayName("order-service WITHOUT config-server (optional import): starts with its own values")
    void clientWithoutServer() {
        // port 1 is never a config server: the connection is refused, and "optional:" lets it start anyway
        ConfigurableApplicationContext orders = startOrderService("--spring.config.import=optional:configserver:http://localhost:1");

        assertThat(orders.getEnvironment().getProperty("orders.free-shipping-from")).isEqualTo("1000");
        assertThat(orders.getEnvironment().getProperty("orders.fallback-shipping-fee")).isEqualTo("150");
    }

    private int startConfigServer() {
        ConfigurableApplicationContext server = new SpringApplicationBuilder(ConfigServerApplication.class)
                .profiles("config-server", "native")
                .run("--server.port=0");
        running.add(server);
        return port(server);
    }

    private ConfigurableApplicationContext startOrderService(String configImport) {
        ConfigurableApplicationContext orders = OrderServiceApplication.builder()
                .run("--server.port=0", "--spring.kafka.admin.auto-create=false", configImport);
        running.add(orders);
        return orders;
    }

    private static int port(ConfigurableApplicationContext context) {
        return Integer.parseInt(context.getEnvironment().getProperty("local.server.port"));
    }
}
