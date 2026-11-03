package jbdl.session11.enrollment;

/**
 * Checked (extends Exception): by default @Transactional does NOT roll back for checked exceptions,
 * a classic surprise. EnrollmentService shows both the surprise and the fix (rollbackFor).
 */
public class NotificationFailedException extends Exception {

    private static final long serialVersionUID = 1L;

    public NotificationFailedException(String message) {
        super(message);
    }
}
