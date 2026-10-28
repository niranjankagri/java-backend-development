package jbdl.splitwise.common;

/** Something asked for by id doesn't exist: becomes HTTP 404. */
public class NotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }
}
