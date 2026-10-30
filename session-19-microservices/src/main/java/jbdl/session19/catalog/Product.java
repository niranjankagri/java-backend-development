package jbdl.session19.catalog;

import java.math.BigDecimal;

/**
 * One product as catalog-service sends it. This JSON shape is the service's CONTRACT with its
 * consumers: removing or renaming a field can break them; adding one cannot (see topic 5).
 *
 * @param servedBy which catalog instance answered, so you can watch the load balancer at work
 */
public record Product(long id, String name, BigDecimal price, int stock, String servedBy) {
}
