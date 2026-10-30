package jbdl.session19.orders;

import java.math.BigDecimal;

/**
 * An order. Name and unit price are COPIED from the catalog when the order is placed: later price
 * changes in the catalog must not change orders already placed, and showing an order then needs no
 * call to catalog-service.
 *
 * @param pricedBy the catalog instance that answered (shows the load balancing)
 */
public record Order(long id, long productId, String productName, int quantity,
                    BigDecimal unitPrice, BigDecimal total, String pricedBy) {
}
