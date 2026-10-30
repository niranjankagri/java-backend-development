package jbdl.session19.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** catalog-service's API: read-only product data for other services (and anyone else). */
@RestController
public class CatalogController {

    private final ProductCatalog catalog;

    public CatalogController(ProductCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/products")
    public List<Product> all() {
        return catalog.all();
    }

    /** 200 with the product, or 404 with a Problem Details body (Session 8). */
    @GetMapping("/products/{id}")
    public ResponseEntity<?> one(@PathVariable long id) {
        // ResponseEntity<?>: the body is either a Product or a ProblemDetail
        Optional<Product> product = catalog.find(id);
        if (product.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No product with id " + id));
        }
        return ResponseEntity.ok(product.get());
    }
}
