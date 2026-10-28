package jbdl.splitwise.common;

/** The request is well-formed but breaks a rule (duplicate email, not a member...): becomes HTTP 409. */
public class BusinessRuleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }
}
