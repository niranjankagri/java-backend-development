package jbdl.session19.orders;

/**
 * catalog-service could not be reached, or answered with a server error. Not the caller's fault,
 * so it becomes 503 Service Unavailable ("try again later"), not 500.
 */
public class CatalogUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CatalogUnavailableException(Throwable cause) {
        super("The catalog service is not available right now. Try again later.", cause);
    }
}
