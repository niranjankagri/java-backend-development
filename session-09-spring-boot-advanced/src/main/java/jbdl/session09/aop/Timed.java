package jbdl.session09.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method whose execution time should be logged. The annotation does nothing by itself;
 * PerformanceAspect finds it at run time (hence RUNTIME retention) and wraps the call.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Timed {
}
