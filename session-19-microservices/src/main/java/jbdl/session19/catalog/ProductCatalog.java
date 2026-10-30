package jbdl.session19.catalog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The products this service owns. A fixed list in memory keeps the session about services, not
 * databases; a real catalog-service would have its OWN database that no other service reads directly
 * ("database per service", topic 1).
 */
@Component
public class ProductCatalog {

    // TreeMap keeps the products sorted by id, so the list endpoint always answers in the same order
    private final Map<Long, Item> items = new TreeMap<>();
    private final String instance;

    public ProductCatalog(@Value("${catalog.instance}") String instance) {
        this.instance = instance;
        add(1, "Keyboard", "49.90", 25);
        add(2, "Mouse", "19.50", 40);
        add(3, "Monitor", "189.00", 7);
    }

    public Optional<Product> find(long id) {
        Item item = items.get(id);
        if (item == null) {
            return Optional.empty();
        }
        return Optional.of(toProduct(item));
    }

    public List<Product> all() {
        List<Product> result = new ArrayList<>();
        for (Item item : items.values()) {
            result.add(toProduct(item));
        }
        return result;
    }

    private void add(long id, String name, String price, int stock) {
        items.put(id, new Item(id, name, new BigDecimal(price), stock));
    }

    private Product toProduct(Item item) {
        return new Product(item.id(), item.name(), item.price(), item.stock(), instance);
    }

    /** What is stored; servedBy is added only when a product is sent out. */
    private record Item(long id, String name, BigDecimal price, int stock) {
    }
}
