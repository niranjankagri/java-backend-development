package jbdl.session09.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a business action that must leave an entry in the audit trail (see AuditAspect). */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /** The action name written to the trail, e.g. "ORDER_PLACED". */
    String action();
}
