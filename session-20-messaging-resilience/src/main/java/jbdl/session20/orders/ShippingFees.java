package jbdl.session20.orders;

/**
 * Gives the shipping fee to a city. {@link ShippingRates} asks shipping-service; tests use a lambda.
 */
public interface ShippingFees {

    ShippingQuote quote(String city);
}
