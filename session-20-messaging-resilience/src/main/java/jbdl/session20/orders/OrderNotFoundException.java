package jbdl.session20.orders;

/** No order with this id: becomes 404 Not Found. */
public class OrderNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OrderNotFoundException(long id) {
        super("No order with id " + id);
    }
}
