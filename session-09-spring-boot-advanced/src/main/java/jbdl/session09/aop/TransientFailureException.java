package jbdl.session09.aop;

/** A failure that may succeed on the next try; the only kind RetryAspect retries. */
public class TransientFailureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TransientFailureException(String message) {
        super(message);
    }
}
