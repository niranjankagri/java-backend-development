package jbdl.session19.orders;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import jbdl.session19.catalog.CatalogServiceApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * The real thing, on this machine: two catalog-service instances and one order-service, each a
 * separate Spring application on its own free port, talking over real HTTP. Nothing is faked.
 *
 * Settings are passed as command-line arguments ("--name=value"), exactly as with java -jar; they
 * win over the values in the properties files.
 */
class MicroservicesEndToEndTest {

    private final List<ConfigurableApplicationContext> running = new ArrayList<>();
    private ConfigurableApplicationContext catalog1;
    private ConfigurableApplicationContext catalog2;
    private RestClient orderService;

    @BeforeEach
    void startServices() {
        catalog1 = startCatalog("catalog-1");
        catalog2 = startCatalog("catalog-2");

        ConfigurableApplicationContext orders = start(new SpringApplicationBuilder(OrderServiceApplication.class)
                .profiles("orders"),
                "--server.port=0",
                // the catalog instances, at the ports they really got
                "--spring.cloud.discovery.client.simple.instances.catalog-service[0].uri=http://localhost:" + port(catalog1),
                "--spring.cloud.discovery.client.simple.instances.catalog-service[1].uri=http://localhost:" + port(catalog2),
                // check the instances' health often, so the failover test does not wait long
                "--spring.cloud.loadbalancer.health-check.interval=200ms");
        orderService = RestClient.create("http://localhost:" + port(orders));

        // the load balancer knows an instance only after its first health check: wait for both
        waitUntilBothInstancesAreUsed();
    }

    @AfterEach
    void stopServices() {
        for (ConfigurableApplicationContext context : running) {
            context.close();
        }
    }

    @Test
    @DisplayName("Feign + load balancer: the orders are spread over both catalog instances")
    void loadBalancing() {
        // Round robin takes the instances in turn, but NOT always exactly 4 + 4 here: the health check
        // (every 200 ms in this test) hands the load balancer a fresh instance list each time, and the
        // two instances can come back in either order. A turn can then land on the same instance twice.
        // (This test used to expect exactly 2 + 2 of 4 orders and failed about one run in three.)
        // What matters: the orders are spread, both instances take some, and no other answer appears.
        List<String> pricedBy = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            pricedBy.add((String) placeOrder(2, 1).getBody().get("pricedBy"));
        }
        assertThat(pricedBy).containsOnly("catalog-1", "catalog-2");
        assertThat(pricedBy).contains("catalog-1", "catalog-2");
    }

    @Test
    @DisplayName("An order through two services: 201, name and price from the catalog, total computed")
    void placeOrder() {
        ResponseEntity<Map<String, Object>> response = placeOrder(3, 2);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody())
                .containsEntry("productName", "Monitor")
                .containsEntry("unitPrice", 189.0)
                .containsEntry("total", 378.0);

        // the order is stored in order-service: reading it back needs no call to the catalog
        String location = response.getHeaders().getLocation().toString();
        Map<String, Object> stored = orderService.get().uri(location).retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(stored).isEqualTo(response.getBody());
    }

    @Test
    @DisplayName("The catalog says 404: order-service answers 404 too, not 500")
    void unknownProduct() {
        ResponseEntity<Map<String, Object>> response = placeOrder(99, 1);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).containsEntry("detail", "Product 99 does not exist in the catalog");
    }

    @Test
    @DisplayName("One catalog instance stops: after the next health check, every order goes to the other")
    void failover() {
        catalog2.close();

        // a few calls may still pick catalog-2 before the health check notices; then all succeed
        waitUntil(() -> placeOrder(1, 1).getStatusCode().value() == 201
                && placeOrder(1, 1).getStatusCode().value() == 201
                && placeOrder(1, 1).getStatusCode().value() == 201);
        for (int i = 0; i < 4; i++) {
            ResponseEntity<Map<String, Object>> response = placeOrder(1, 1);
            assertThat(response.getStatusCode().value()).isEqualTo(201);
            assertThat(response.getBody()).containsEntry("pricedBy", "catalog-1");
        }
    }

    @Test
    @DisplayName("Both catalog instances down: 503 Service Unavailable, not 500")
    void catalogDown() {
        catalog1.close();
        catalog2.close();

        ResponseEntity<Map<String, Object>> response = placeOrder(1, 1);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).containsEntry("title", "Catalog unavailable");
    }

    // ---- helpers ----

    private ConfigurableApplicationContext startCatalog(String instanceName) {
        return start(new SpringApplicationBuilder(CatalogServiceApplication.class).profiles("catalog"),
                "--server.port=0", "--catalog.instance=" + instanceName);
    }

    private ConfigurableApplicationContext start(SpringApplicationBuilder builder, String... args) {
        ConfigurableApplicationContext context = builder.run(args);
        running.add(context);
        return context;
    }

    /** The port the app really got (server.port=0 means "any free port"). */
    private static int port(ConfigurableApplicationContext context) {
        return Integer.parseInt(context.getEnvironment().getProperty("local.server.port"));
    }

    /** POST /orders; answers with any status are returned, not thrown, so the tests can check them. */
    private ResponseEntity<Map<String, Object>> placeOrder(long productId, int quantity) {
        return orderService.post()
                .uri("/orders")
                .body(Map.of("productId", productId, "quantity", quantity))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> { /* keep error answers */ })
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /** The health check finds the instances shortly after start; wait until both have answered once. */
    private void waitUntilBothInstancesAreUsed() {
        Set<Object> seen = new HashSet<>();
        waitUntil(() -> {
            seen.add(placeOrder(1, 1).getBody().get("pricedBy"));
            return seen.contains("catalog-1") && seen.contains("catalog-2");
        });
    }

    /** Tries the check every 100 ms for up to 10 s (a tiny version of the Awaitility library). */
    private static void waitUntil(BooleanSupplier check) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (!check.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("condition not reached within 10 s");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted", e);
            }
        }
    }
}
