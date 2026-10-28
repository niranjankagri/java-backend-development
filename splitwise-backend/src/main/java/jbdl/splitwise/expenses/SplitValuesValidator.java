package jbdl.splitwise.expenses;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jbdl.splitwise.expenses.ExpenseDtos.AddExpense;
import jbdl.splitwise.expenses.ExpenseDtos.Participant;

/**
 * The code behind @ValidSplitValues. Bean Validation calls isValid with the whole request; returning
 * false produces a 400 whose error message says exactly what is wrong.
 */
public class SplitValuesValidator implements ConstraintValidator<ValidSplitValues, AddExpense> {

    @Override
    public boolean isValid(AddExpense request, ConstraintValidatorContext context) {
        if (request.splitType() == null || request.participants() == null) {
            return true; // @NotNull / @NotEmpty on those fields report that problem already
        }
        for (Participant participant : request.participants()) {
            boolean hasValue = participant.value() != null;
            if (request.splitType() == SplitType.EQUAL && hasValue) {
                return fail(context, "an EQUAL split takes no values");
            }
            if (request.splitType() != SplitType.EQUAL && (!hasValue || participant.value().signum() <= 0)) {
                return fail(context, "a " + request.splitType() + " split needs a positive value for every participant");
            }
        }
        return true;
    }

    /** Replaces the default message with a specific one, reported on the "participants" field. */
    private static boolean fail(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addPropertyNode("participants").addConstraintViolation();
        return false;
    }
}
