package jbdl.session20.orders;

import java.math.BigDecimal;

/** The body of POST /orders: {"customerEmail":"asha@example.com","city":"Delhi","amount":450} */
public record OrderRequest(String customerEmail, String city, BigDecimal amount) {
}
