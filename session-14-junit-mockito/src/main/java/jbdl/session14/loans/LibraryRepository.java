package jbdl.session14.loans;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Where books and loans are stored. An INTERFACE, so the service doesn't care whether the data is in
 * memory (InMemoryLibraryRepository), in a database, or in a Mockito mock during a unit test.
 */
public interface LibraryRepository {

    Optional<Book> findBook(long bookId);

    Optional<Loan> findLoan(long loanId);

    /** Stores a new loan and returns it with its id. */
    Loan saveLoan(long bookId, String member, LocalDate borrowedOn, LocalDate dueOn);

    void deleteLoan(long loanId);

    /** How many books the member has right now. */
    int countLoansOf(String member);
}
