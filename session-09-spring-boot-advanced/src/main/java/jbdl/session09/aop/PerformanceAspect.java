package jbdl.session09.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Around advice: runs INSTEAD of the method, decides when to call it (proceed()) and can measure,
 * change the arguments or the result, or catch exceptions. Here: log how long every @Timed method took.
 */
@Slf4j
@Aspect
@Component
public class PerformanceAspect {

    /**
     * Pointcut "@annotation(timed)": every method annotated with @Timed. The name `timed` matches the
     * parameter below, so Spring also passes in the annotation itself (not used here, but it could carry
     * settings, as @Retryable's maxAttempts does).
     *
     * Without AOP you would copy these lines into every method you want to time:
     *   long start = System.nanoTime(); try { ...real work... } finally { log the time }
     */
    @Around("@annotation(timed)")
    public Object time(ProceedingJoinPoint call, Timed timed) throws Throwable {
        long start = System.nanoTime();
        try {
            return call.proceed(); // runs the real method (or the next aspect in the chain)
        } finally {
            long micros = (System.nanoTime() - start) / 1_000;
            log.info("{} took {} us", call.getSignature().toShortString(), micros);
        }
    }
}
