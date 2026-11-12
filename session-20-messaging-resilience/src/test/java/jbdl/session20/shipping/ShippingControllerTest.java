package jbdl.session20.shipping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** shipping-service on its own, through MockMvc (no real HTTP). */
@SpringBootTest(classes = ShippingServiceApplication.class)
@ActiveProfiles("shipping")
@AutoConfigureMockMvc
class ShippingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("GET /rates/Delhi: the city's own fee (the city name in any case)")
    void knownCity() throws Exception {
        mvc.perform(get("/rates/Delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Delhi"))
                .andExpect(jsonPath("$.fee").value(40));
    }

    @Test
    @DisplayName("GET /rates/Pune: a city without its own fee pays the standard 120")
    void otherCity() throws Exception {
        mvc.perform(get("/rates/Pune"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fee").value(120));
    }
}
