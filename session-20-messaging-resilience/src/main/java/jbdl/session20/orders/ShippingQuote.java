package jbdl.session20.orders;

import java.math.BigDecimal;

/**
 * A shipping fee and where it came from.
 *
 * @param source "shipping-service" (the real fee), "free" (order above the free-shipping amount), or
 *               "fallback: ..." (shipping-service could not answer; the reason follows the colon)
 */
public record ShippingQuote(BigDecimal fee, String source) {
}
