package jbdl.session09;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jbdl.session09.logging.RequestIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Topic 4 (logging): every request gets an id, the id is in the response header and in every log line
 * written while handling that request, and it is cleaned up afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class LoggingTest {

    private static final String ORDER = """
            {"customer": "Asha", "lines": [{"product": "pen", "quantity": 2, "unitPrice": 25.00}]}""";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("A new id is created and returned when the client sends none")
    void generatesId() throws Exception {
        MvcResult result = mvc.perform(get("/api/orders/999")).andExpect(status().isNotFound()).andReturn();

        // a UUID looks like 8-4-4-4-12 hex digits
        assertThat(result.getResponse().getHeader(RequestIdFilter.HEADER)).matches("[0-9a-f-]{36}");
    }

    @Test
    @DisplayName("The client's id is reused, and appears in the log lines of that request")
    void reusesIdInLogs(CapturedOutput output) throws Exception {
        mvc.perform(post("/api/orders").header(RequestIdFilter.HEADER, "checkout-42")
                        .contentType(MediaType.APPLICATION_JSON).content(ORDER))
                .andExpect(status().isCreated())
                .andExpect(header().string(RequestIdFilter.HEADER, "checkout-42"));

        // the pattern in application.properties prints [%X{requestId}] on every line
        assertThat(output.getOut()).contains("[checkout-42] jbdl.session09.orders.OrderService - order");
    }

    @Test
    @DisplayName("An unsafe id (spaces, symbols) is ignored and replaced")
    void rejectsUnsafeId() throws Exception {
        MvcResult result = mvc.perform(get("/api/orders/1").header(RequestIdFilter.HEADER, "x\n<script>"))
                .andReturn();

        assertThat(result.getResponse().getHeader(RequestIdFilter.HEADER)).doesNotContain("script");
    }

    @Test
    @DisplayName("After the request the id is removed from the MDC (no leak into the next request)")
    void cleansUp() throws Exception {
        mvc.perform(get("/api/orders/1").header(RequestIdFilter.HEADER, "abc-1"));

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }
}
