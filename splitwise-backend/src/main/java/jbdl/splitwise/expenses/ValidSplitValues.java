package jbdl.splitwise.expenses;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Session 17, advanced validation: a CUSTOM constraint for a rule that involves several fields at once,
 * which @NotNull or @Positive on single fields can't express:
 *   EQUAL           -> participants must NOT have values
 *   EXACT, PERCENT  -> every participant MUST have a positive value
 * It sits on the whole request record (TYPE) and is checked by SplitValuesValidator with @Valid, so a
 * wrong request gets a 400 before any service code runs.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SplitValuesValidator.class)
public @interface ValidSplitValues {

    String message() default "participant values don't match the split type";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
