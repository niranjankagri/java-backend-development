package jbdl.session19.orders;

/** No order with this id in order-service. */
public class OrderNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OrderNotFoundException(long orderId) {
        super("No order with id " + orderId);
    }
}
