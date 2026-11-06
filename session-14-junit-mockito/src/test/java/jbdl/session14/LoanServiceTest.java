package jbdl.session14;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.Optional;
import jbdl.session14.loans.Book;
import jbdl.session14.loans.LibraryRepository;
import jbdl.session14.loans.Loan;
import jbdl.session14.loans.LoanService;
import jbdl.session14.loans.Notifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Topics 2-3 (Mockito, service-layer tests): LoanService tested ALONE. Its two collaborators are
 * Mockito mocks: fake objects that return what we tell them (when...thenReturn) and remember how they
 * were called (verify). No Spring, no database, no messages sent: each test runs in milliseconds.
 * The clock is a real but FIXED clock, so "today" is always 1 Oct 2026.
 *
 * Every test follows given / when / then: set up the mocks, call the service, check the result and
 * the calls.
 */
@ExtendWith(MockitoExtension.class) // creates the @Mock fields before each test
class LoanServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private LibraryRepository repository;

    @Mock
    private Notifier notifier;

    private LoanService service;

    @BeforeEach
    void createService() {
        // built by hand: easy to read, and a real (fixed) Clock can't be injected by @InjectMocks
        service = new LoanService(repository, notifier, FIXED_CLOCK);
    }

    @Test
    @DisplayName("borrow: saves a 14-day loan, takes a copy, and notifies the member")
    void borrowHappyPath() {
        // given
        Book cleanCode = new Book(1, "Clean Code", 2);
        when(repository.countLoansOf("Asha")).thenReturn(0);
        when(repository.findBook(1)).thenReturn(Optional.of(cleanCode));
        when(repository.saveLoan(1, "Asha", TODAY, TODAY.plusDays(14)))
                .thenReturn(new Loan(42, 1, "Asha", TODAY, TODAY.plusDays(14)));

        // when
        Loan loan = service.borrow(1, "Asha");

        // then
        assertThat(loan.id()).isEqualTo(42);
        assertThat(cleanCode.getCopiesAvailable()).isEqualTo(1);
        verify(notifier).send("Asha", "You borrowed \"Clean Code\". Please return it by 2026-10-15.");
    }

    @Test
    @DisplayName("borrow: ArgumentCaptor catches what the service passed to a mock")
    void borrowCapturesDueDate() {
        when(repository.findBook(1)).thenReturn(Optional.of(new Book(1, "Clean Code", 1)));
        when(repository.saveLoan(anyLong(), anyString(), any(), any())).thenReturn(new Loan(1, 1, "Ravi", TODAY, TODAY));

        service.borrow(1, "Ravi");

        ArgumentCaptor<LocalDate> dueDate = ArgumentCaptor.forClass(LocalDate.class);
        verify(repository).saveLoan(eq(1L), eq("Ravi"), eq(TODAY), dueDate.capture());
        assertThat(dueDate.getValue()).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    @DisplayName("borrow: a member with 3 books is refused before the book is even looked up")
    void borrowLimit() {
        when(repository.countLoansOf("Meena")).thenReturn(3);

        assertThatThrownBy(() -> service.borrow(1, "Meena"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Meena already has 3 books");
        verify(repository, never()).findBook(anyLong());   // stopped early
        verifyNoInteractions(notifier);                     // no message sent
    }

    @Test
    @DisplayName("borrow: no copy on the shelf -> no loan saved, no message")
    void borrowNoCopy() {
        when(repository.findBook(3)).thenReturn(Optional.of(new Book(3, "Head First Java", 0)));

        assertThatThrownBy(() -> service.borrow(3, "Asha")).isInstanceOf(IllegalStateException.class);
        verify(repository, never()).saveLoan(anyLong(), anyString(), any(), any());
        verifyNoInteractions(notifier);
    }

    @Test
    @DisplayName("borrow: an unknown book -> NoSuchElementException")
    void borrowUnknownBook() {
        when(repository.findBook(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.borrow(99, "Asha"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("book 99 not found");
    }

    @Test
    @DisplayName("giveBack on time: no fine and no message")
    void giveBackOnTime() {
        Book book = new Book(1, "Clean Code", 0);
        when(repository.findLoan(5)).thenReturn(Optional.of(new Loan(5, 1, "Asha", TODAY.minusDays(10), TODAY.plusDays(4))));
        when(repository.findBook(1)).thenReturn(Optional.of(book));

        int fine = service.giveBack(5);

        assertThat(fine).isZero();
        assertThat(book.getCopiesAvailable()).isEqualTo(1);
        verify(repository).deleteLoan(5);
        verifyNoInteractions(notifier);
    }

    @Test
    @DisplayName("giveBack 3 days late: 30 rupees, the loan is deleted BEFORE the member is told")
    void giveBackLate() {
        when(repository.findLoan(6)).thenReturn(Optional.of(new Loan(6, 1, "Ravi", TODAY.minusDays(17), TODAY.minusDays(3))));
        when(repository.findBook(1)).thenReturn(Optional.of(new Book(1, "Clean Code", 0)));

        int fine = service.giveBack(6);

        assertThat(fine).isEqualTo(30);
        InOrder order = inOrder(repository, notifier); // checks the ORDER of calls across mocks
        order.verify(repository).deleteLoan(6);
        order.verify(notifier).send("Ravi", "Returned late: please pay a fine of 30 rupees.");
    }
}
