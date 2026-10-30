package jbdl.session19.catalog;

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

/**
 * catalog-service on its own: a microservice is a normal Spring Boot app and is tested like one,
 * without the services that call it. @ActiveProfiles("catalog") loads its settings, as main() does.
 */
@SpringBootTest(classes = CatalogServiceApplication.class, properties = "catalog.instance=catalog-test")
@ActiveProfiles("catalog")
@AutoConfigureMockMvc
class CatalogControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("GET /products/2: the product, and which instance answered")
    void oneProduct() throws Exception {
        mvc.perform(get("/products/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Mouse"))
                .andExpect(jsonPath("$.price").value(19.50))
                .andExpect(jsonPath("$.stock").value(40))
                .andExpect(jsonPath("$.servedBy").value("catalog-test"));
    }

    @Test
    @DisplayName("GET /products: all products, sorted by id")
    void allProducts() throws Exception {
        mvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Keyboard"))
                .andExpect(jsonPath("$[2].name").value("Monitor"));
    }

    @Test
    @DisplayName("Unknown product: 404 with a Problem Details body")
    void unknownProduct() throws Exception {
        mvc.perform(get("/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No product with id 99"));
    }
}
