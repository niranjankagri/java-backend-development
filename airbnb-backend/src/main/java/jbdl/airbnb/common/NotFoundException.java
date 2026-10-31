package jbdl.airbnb.common;

/** Something with this id does not exist: 404 Not Found. */
public class NotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }
}
