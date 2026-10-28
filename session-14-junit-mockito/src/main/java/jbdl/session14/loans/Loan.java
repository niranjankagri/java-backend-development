package jbdl.session14.loans;

import java.time.LocalDate;

/** One borrowed book: who has it and until when. */
public record Loan(long id, long bookId, String member, LocalDate borrowedOn, LocalDate dueOn) {

    /** Days late on a given date (0 if returned in time). */
    public long daysLate(LocalDate returnedOn) {
        if (!returnedOn.isAfter(dueOn)) {
            return 0;
        }
        return returnedOn.toEpochDay() - dueOn.toEpochDay();
    }
}
