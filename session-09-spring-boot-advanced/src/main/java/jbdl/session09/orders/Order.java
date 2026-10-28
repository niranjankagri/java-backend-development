package jbdl.session09.orders;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * Lombok @Value: all fields private final, getters, equals/hashCode, toString and an all-args
 * constructor, generated at compile time. @Builder adds Order.builder()...build(); @Singular lets the
 * builder add lines one at a time (.line(a).line(b)) and makes the list immutable.
 * (A Java record would do the same here without Lombok; @Value shines on classes that need more.)
 */
@Value
@Builder
public class Order {

    // With @Value, Lombok makes these private and final; written out it would be
    // `private final long id;` plus a getId() method, and so on for each field.
    long id;
    String customer;
    @Singular // the builder gets .line(x) to add one line at a time, besides .lines(list)
    List<OrderLine> lines;
    Status status;

    /** Where the order is in its life. */
    public enum Status { PLACED, PAID, CANCELLED }

    /** One product line. Lombok @Value works on small nested classes too. */
    @Value
    public static class OrderLine {
        String product;
        int quantity;
        BigDecimal unitPrice;

        public BigDecimal total() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    /** Sum of the line totals. */
    public BigDecimal total() {
        // Simple version: start at zero and add each line. With streams (Session 4) the same is
        //   lines.stream().map(OrderLine::total).reduce(BigDecimal.ZERO, BigDecimal::add)
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderLine line : lines) {
            sum = sum.add(line.total()); // BigDecimal is immutable: add() returns a new value
        }
        return sum;
    }

    /** @Value classes are immutable: a status change returns a copy (toBuilder would also work). */
    public Order withStatus(Status newStatus) {
        return new Order(id, customer, lines, newStatus);
    }
}
