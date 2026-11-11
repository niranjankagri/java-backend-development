package jbdl.session19.orders;

/** catalog-service answered 404: there is no such product. Becomes a 404 for our caller too. */
public class ProductNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProductNotFoundException(long productId) {
        super("Product " + productId + " does not exist in the catalog");
    }
}
