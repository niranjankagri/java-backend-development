package jbdl.session19.catalog;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * catalog-service: knows the products and their prices, and nothing else. It PRODUCES product data;
 * order-service CONSUMES it over HTTP.
 *
 * Run two instances, so order-service has something to balance between (see the session README):
 *   java -jar target/session-19-microservices-1.0.0-SNAPSHOT-catalog.jar --server.port=8191 --catalog.instance=catalog-1
 *   java -jar target/session-19-microservices-1.0.0-SNAPSHOT-catalog.jar --server.port=8192 --catalog.instance=catalog-2
 */
@SpringBootApplication
public class CatalogServiceApplication {

    public static void main(String[] args) {
        // The "catalog" profile loads application-catalog.properties: this service's own settings.
        // (Both services share one module here; in real life each has its own application.properties.)
        new SpringApplicationBuilder(CatalogServiceApplication.class).profiles("catalog").run(args);
    }
}
