package jbdl.session14;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import jbdl.session14.loans.Loan;
import jbdl.session14.loans.LoanController;
import jbdl.session14.loans.LoanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Topics 4-5 (MockMvc, controller-layer tests): @WebMvcTest starts ONLY the web layer for
 * LoanController (JSON, validation, exception handlers), not the services or repositories. LoanService
 * is replaced by a Mockito mock (@MockitoBean), so these tests check the HTTP mapping only:
 * URL and method, status codes, JSON in and out, validation, error responses.
 */
@WebMvcTest(LoanController.class)
class LoanControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean // a mock registered as a Spring bean, injected into the controller
    private LoanService service;

    @Test
    @DisplayName("POST /api/loans: 201 and the loan as JSON")
    void borrow() throws Exception {
        when(service.borrow(1, "Asha"))
                .thenReturn(new Loan(42, 1, "Asha", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15)));

        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content("{\"bookId\": 1, \"member\": \"Asha\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.dueOn").value("2026-10-15"));
    }

    @Test
    @DisplayName("Invalid body: 400, and the service is never called")
    void invalidBody() throws Exception {
        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content("{\"member\": \"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Service says 'not found' -> 404 with the message")
    void notFound() throws Exception {
        when(service.borrow(anyLong(), anyString())).thenThrow(new NoSuchElementException("book 99 not found"));

        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content("{\"bookId\": 99, \"member\": \"Asha\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("book 99 not found"));
    }

    @Test
    @DisplayName("Service says 'limit reached' -> 409")
    void conflict() throws Exception {
        when(service.borrow(anyLong(), anyString())).thenThrow(new IllegalStateException("Meena already has 3 books"));

        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content("{\"bookId\": 1, \"member\": \"Meena\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /api/loans/{id}: the fine from the service in the JSON")
    void giveBack() throws Exception {
        when(service.giveBack(6)).thenReturn(30);

        mvc.perform(delete("/api/loans/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value(6))
                .andExpect(jsonPath("$.fineInRupees").value(30));
        verify(service).giveBack(6);
    }
}
