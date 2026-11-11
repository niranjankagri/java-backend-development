package jbdl.session19.orders;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * The HTTP client for catalog-service, written as a plain interface. Feign creates the class that
 * sends the requests: catalog.product(2) becomes GET http://(a catalog instance)/products/2, and the
 * JSON answer becomes a CatalogProduct.
 *
 * name = "catalog-service" is a SERVICE NAME, not an address. The load balancer turns it into one of
 * the instances listed in application-orders.properties, a different one each call (topic 4).
 *
 * Without Feign the same call is a few lines of RestClient (Session 9):
 *   restClient.get().uri("http://localhost:8191/products/{id}", id).retrieve().body(CatalogProduct.class);
 */
@FeignClient(name = "catalog-service")
public interface CatalogClient {

    @GetMapping("/products/{id}")
    CatalogProduct product(@PathVariable("id") long id);
}
