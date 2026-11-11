package jbdl.session19.orders;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * order-service: takes orders. It does not own any product data; for every order it ASKS
 * catalog-service for the product and its current price (through {@link CatalogClient}).
 *
 * Start the two catalog instances first, then:
 *   java -jar target/session-19-microservices-1.0.0-SNAPSHOT-orders.jar
 */
@SpringBootApplication
// Finds the @FeignClient interfaces in this package and creates an HTTP client for each
@EnableFeignClients
public class OrderServiceApplication {

    public static void main(String[] args) {
        // The "orders" profile loads application-orders.properties (port, catalog instances).
        new SpringApplicationBuilder(OrderServiceApplication.class).profiles("orders").run(args);
    }

    /**
     * The HTTP client the load balancer's health check uses to call each catalog instance's
     * /actuator/health (spring.cloud.loadbalancer.configurations=health-check). Without this bean the
     * health check is not set up, and the load balancer finds no instances at all.
     */
    @Bean
    RestClient healthCheckRestClient() {
        return RestClient.create();
    }
}
