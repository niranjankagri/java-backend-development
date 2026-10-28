package jbdl.session15;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The browser side: HTTP caching headers (Cache-Control, ETag, 304 Not Modified) and a cookie.
 * MockMvc plays the browser: it sends the headers a browser would send.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BrowserCacheAndCookieTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("A rate comes with Cache-Control max-age=60 and an ETag")
    void cachingHeaders() throws Exception {
        mvc.perform(get("/api/rates/eur"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=60"))
                .andExpect(header().string("ETag", "\"EUR-90.10\""));
    }

    @Test
    @DisplayName("Asking again with the same ETag: 304 Not Modified and no body")
    void notModified() throws Exception {
        MvcResult result = mvc.perform(get("/api/rates/usd").header("If-None-Match", "\"USD-83.20\""))
                .andExpect(status().isNotModified())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }

    @Test
    @DisplayName("After the rate changes, the old ETag no longer matches: 200 with the new value")
    void changedRate() throws Exception {
        mvc.perform(put("/api/rates/gbp").contentType(MediaType.APPLICATION_JSON).content("107.00"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/rates/gbp").header("If-None-Match", "\"GBP-105.40\""))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"GBP-107.00\""));
    }

    @Test
    @DisplayName("Cookie: the server sets it (HttpOnly, SameSite, 30 days); the browser sends it back")
    void cookie() throws Exception {
        String setCookie = mvc.perform(put("/api/preferences/currency/eur"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("Set-Cookie");

        assertThat(setCookie)
                .startsWith("currency=EUR")
                .contains("Max-Age=2592000") // 30 days in seconds
                .contains("HttpOnly")
                .contains("SameSite=Lax");

        // the browser sends the cookie back with later requests
        mvc.perform(get("/api/preferences/currency").cookie(new Cookie("currency", "EUR")))
                .andExpect(content().string("EUR"));
        // a first-time visitor has no cookie: the default applies
        mvc.perform(get("/api/preferences/currency"))
                .andExpect(content().string("INR"));
    }
}
