package jbdl.session20.orders;

import java.math.BigDecimal;

/**
 * A placed order.
 *
 * @param shippingFeeSource where the fee came from (see {@link ShippingQuote#source()}), so a
 *                          fallback fee is visible in the answer
 * @param total             amount + shipping fee
 */
public record Order(long id, String customerEmail, String city, BigDecimal amount,
                    BigDecimal shippingFee, String shippingFeeSource, BigDecimal total) {
}
