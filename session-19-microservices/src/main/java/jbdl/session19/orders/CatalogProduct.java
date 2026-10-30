package jbdl.session19.orders;

import java.math.BigDecimal;

/**
 * A product as order-service READS it from catalog-service. It is order-service's own class, not a
 * copy of the catalog's Product: it keeps only the fields this consumer needs. The catalog also sends
 * "stock"; it is ignored here, so the catalog may add fields without breaking us ("tolerant reader",
 * topic 5).
 */
public record CatalogProduct(long id, String name, BigDecimal price, String servedBy) {
}
