package jbdl.session09.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Retry the method when it throws TransientFailureException (a network blip, a busy service).
 * Only for idempotent calls: a retried non-idempotent call can do the work twice.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retryable {

    /** Total attempts, including the first one. */
    int maxAttempts() default 3;
}
