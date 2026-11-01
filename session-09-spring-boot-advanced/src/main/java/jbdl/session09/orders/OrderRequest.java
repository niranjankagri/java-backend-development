package jbdl.session09.orders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lombok @Data: getters, SETTERS, equals/hashCode, toString and a constructor for final fields: a
 * mutable bean. Jackson can fill it through the no-args constructor and the setters. Prefer @Value or
 * records for anything that shouldn't change after creation; @Data is for simple mutable carriers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    private String customer;
    private List<Line> lines = new ArrayList<>();

    /** A requested line, also a mutable @Data bean. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line {
        private String product;
        private int quantity;
        private BigDecimal unitPrice;
    }
}
