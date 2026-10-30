package jbdl.session19.orders;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns order-service's exceptions into Problem Details answers (Session 8). */
@RestControllerAdvice
public class OrderExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail productNotFound(ProductNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Product not found", e.getMessage());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail orderNotFound(OrderNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Order not found", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid order", e.getMessage());
    }

    /** 503: the problem is a service we depend on, and it may work again in a moment. */
    @ExceptionHandler(CatalogUnavailableException.class)
    public ProblemDetail catalogUnavailable(CatalogUnavailableException e) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Catalog unavailable", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
