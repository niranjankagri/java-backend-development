package jbdl.session14.loans;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HTTP for loans. The controller tests replace LoanService with a mock and check only this class. */
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService service;

    public LoanController(LoanService service) {
        this.service = service;
    }

    /** Body of POST /api/loans. */
    public record BorrowRequest(@NotNull Long bookId, @NotBlank String member) {
    }

    /** Response of DELETE /api/loans/{id}: the fine to pay (0 if on time). */
    public record ReturnResult(long loanId, int fineInRupees) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Loan borrow(@Valid @RequestBody BorrowRequest request) {
        return service.borrow(request.bookId(), request.member());
    }

    @DeleteMapping("/{loanId}")
    public ReturnResult giveBack(@PathVariable long loanId) {
        return new ReturnResult(loanId, service.giveBack(loanId));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail notFound(NoSuchElementException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail conflict(IllegalStateException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
