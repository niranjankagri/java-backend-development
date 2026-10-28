package jbdl.session14;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * For comparison: an INTEGRATION test. The whole application starts with its real beans (no mocks),
 * and one request goes through controller, service and repository together. Slower and less precise
 * than the unit tests, but it proves the parts are wired together correctly. Keep only a few of these.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LibraryApplicationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Borrowing a book from the shelf of the real in-memory library works end to end")
    void borrowForReal() throws Exception {
        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content("{\"bookId\": 2, \"member\": \"Asha\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(2));

        // Effective Java had 1 copy: a second borrower is refused by the real service
        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content("{\"bookId\": 2, \"member\": \"Ravi\"}"))
                .andExpect(status().isConflict());
    }
}
