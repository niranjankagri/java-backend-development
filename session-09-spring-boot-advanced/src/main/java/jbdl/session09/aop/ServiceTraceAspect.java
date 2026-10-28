package jbdl.session09.aop;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * A pointcut by location instead of by annotation: every public method of every @Service class in
 * this application. @Before runs before the method and can't stop it (except by throwing).
 * Logged at DEBUG, so it shows only when logging.level.jbdl.session09 is DEBUG.
 */
@Slf4j
@Aspect
@Component
public class ServiceTraceAspect {

    /**
     * A named, reusable pointcut. Read it in two parts:
     *   within(@org.springframework.stereotype.Service jbdl.session09..*)  = inside any class annotated
     *                                    @Service, in package jbdl.session09 or below (".." = sub-packages)
     *   execution(public * *(..))       = any public method: any return type (*), any name (*), any arguments (..)
     * and && means both must hold. Simpler alternative: put a custom annotation on each method you want
     * (like @Timed) and use "@annotation(...)", as the other aspects do.
     */
    @Pointcut("within(@org.springframework.stereotype.Service jbdl.session09..*) && execution(public * *(..))")
    void publicServiceMethods() {
    }

    @Before("publicServiceMethods()")
    public void trace(JoinPoint call) {
        log.debug("-> {} {}", call.getSignature().toShortString(), Arrays.toString(call.getArgs()));
    }
}
