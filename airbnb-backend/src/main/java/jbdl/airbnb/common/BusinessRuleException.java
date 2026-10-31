package jbdl.airbnb.common;

/** The request is valid, but breaks a rule of the business (e.g. an email registered twice): 409 Conflict. */
public class BusinessRuleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }
}
