package jbdl.session14.loans;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

/** The real implementation used when the application runs: a few books in memory. */
@Repository
public class InMemoryLibraryRepository implements LibraryRepository {

    private final Map<Long, Book> books = new ConcurrentHashMap<>();
    private final Map<Long, Loan> loans = new ConcurrentHashMap<>();
    private final AtomicLong nextLoanId = new AtomicLong(1);

    public InMemoryLibraryRepository() {
        books.put(1L, new Book(1, "Clean Code", 2));
        books.put(2L, new Book(2, "Effective Java", 1));
        books.put(3L, new Book(3, "Head First Java", 0));
    }

    @Override
    public Optional<Book> findBook(long bookId) {
        return Optional.ofNullable(books.get(bookId));
    }

    @Override
    public Optional<Loan> findLoan(long loanId) {
        return Optional.ofNullable(loans.get(loanId));
    }

    @Override
    public Loan saveLoan(long bookId, String member, LocalDate borrowedOn, LocalDate dueOn) {
        Loan loan = new Loan(nextLoanId.getAndIncrement(), bookId, member, borrowedOn, dueOn);
        loans.put(loan.id(), loan);
        return loan;
    }

    @Override
    public void deleteLoan(long loanId) {
        loans.remove(loanId);
    }

    @Override
    public int countLoansOf(String member) {
        int count = 0;
        for (Loan loan : loans.values()) {
            if (loan.member().equals(member)) {
                count++;
            }
        }
        return count;
    }
}
