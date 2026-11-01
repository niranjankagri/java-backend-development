package jbdl.session09.aop;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Two more advice types: @AfterReturning runs only when the method returned normally (and can see
 * the result); @AfterThrowing only when it threw (and can see the exception). Neither can change
 * the outcome, which makes them the right choice for auditing.
 */
@Aspect
@Component
@RequiredArgsConstructor // Lombok: a constructor for the final field, used by Spring for injection
public class AuditAspect {

    private final AuditTrail trail;

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void success(Audited audited, Object result) {
        trail.record(new AuditTrail.Entry(audited.action(), String.valueOf(result), true));
    }

    @AfterThrowing(pointcut = "@annotation(audited)", throwing = "error")
    public void failure(JoinPoint call, Audited audited, Exception error) {
        String detail = Arrays.toString(call.getArgs()) + " -> " + error.getMessage();
        trail.record(new AuditTrail.Entry(audited.action(), detail, false));
    }
}
