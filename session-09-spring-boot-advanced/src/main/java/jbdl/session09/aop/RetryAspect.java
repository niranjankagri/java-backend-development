package jbdl.session09.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Retries a @Retryable method on TransientFailureException. Any other exception, or the last failed
 * attempt, goes straight to the caller. (Spring Framework 7 also has a ready-made @Retryable; this one
 * is written out to show that an around advice can call proceed() more than once.)
 */
@Slf4j
@Aspect
@Component
public class RetryAspect {

    @Around("@annotation(retryable)")
    public Object retry(ProceedingJoinPoint call, Retryable retryable) throws Throwable {
        String method = call.getSignature().toShortString();
        int attempt = 1;
        while (true) {
            try {
                return call.proceed(); // success: leave the loop with the result
            } catch (TransientFailureException e) {
                if (attempt == retryable.maxAttempts()) {
                    log.warn("{} failed after {} attempts: {}", method, attempt, e.getMessage());
                    throw e; // out of attempts: the caller gets the last error
                }
                log.info("{} attempt {} failed ({}), retrying", method, attempt, e.getMessage());
                attempt++;
            }
        }
    }
}
