package jbdl.session14.loans;

import java.time.Clock;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

/**
 * The business rules (the code the unit tests are about):
 *  - a member may have at most 3 books at a time
 *  - a book can only be borrowed if a copy is on the shelf
 *  - a loan lasts 14 days; returning late costs 10 rupees per day
 *  - the member gets a message when they borrow, and when they pay a fine
 */
@Service
public class LoanService {

    public static final int MAX_BOOKS_PER_MEMBER = 3;
    public static final int LOAN_DAYS = 14;
    public static final int FINE_PER_DAY = 10;

    private final LibraryRepository repository;
    private final Notifier notifier;
    private final Clock clock;

    /** Everything the service needs comes in through the constructor, so a test can pass fakes. */
    public LoanService(LibraryRepository repository, Notifier notifier, Clock clock) {
        this.repository = repository;
        this.notifier = notifier;
        this.clock = clock;
    }

    public Loan borrow(long bookId, String member) {
        if (repository.countLoansOf(member) >= MAX_BOOKS_PER_MEMBER) {
            throw new IllegalStateException(member + " already has " + MAX_BOOKS_PER_MEMBER + " books");
        }
        Book book = repository.findBook(bookId).orElseThrow(() -> new NoSuchElementException("book " + bookId + " not found"));
        book.takeCopy(); // throws IllegalStateException when the shelf is empty

        LocalDate today = LocalDate.now(clock); // "now" from the injected clock, not the computer's
        Loan loan = repository.saveLoan(bookId, member, today, today.plusDays(LOAN_DAYS));
        notifier.send(member, "You borrowed \"" + book.getTitle() + "\". Please return it by " + loan.dueOn() + ".");
        return loan;
    }

    /** Returns the book and the fine in rupees (0 when on time). */
    public int giveBack(long loanId) {
        Loan loan = repository.findLoan(loanId).orElseThrow(() -> new NoSuchElementException("loan " + loanId + " not found"));
        Book book = repository.findBook(loan.bookId()).orElseThrow();
        book.returnCopy();
        repository.deleteLoan(loanId);

        int fine = (int) loan.daysLate(LocalDate.now(clock)) * FINE_PER_DAY;
        if (fine > 0) {
            notifier.send(loan.member(), "Returned late: please pay a fine of " + fine + " rupees.");
        }
        return fine;
    }
}
