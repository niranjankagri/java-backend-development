package jbdl.airbnb.common;

/** The caller may not do this (e.g. change someone else's listing): 403 Forbidden. */
public class ForbiddenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(message);
    }
}
