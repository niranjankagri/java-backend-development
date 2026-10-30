package jbdl.session19.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jbdl.session19.orders.CatalogProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * The CONTRACT between the producer (catalog-service) and its consumer (order-service), checked on
 * the producer's side: "does the catalog still send what order-service reads?"
 *
 * If someone renames "price" to "unitPrice" in the catalog, the catalog's own tests could be updated
 * and pass, while order-service would break in production. This test fails first instead.
 * (Tools like Spring Cloud Contract or Pact do this at scale, across separate projects; topic 5.)
 */
@SpringBootTest(classes = CatalogServiceApplication.class)
@ActiveProfiles("catalog")
@AutoConfigureMockMvc
class CatalogContractTest {

    @Autowired
    private MockMvc mvc;

    // the same JSON settings the services use (Spring Boot's mapper ignores unknown fields)
    @Autowired
    private JsonMapper json;

    @Test
    @DisplayName("Every field order-service reads is there, with the expected type")
    void fieldsTheConsumerNeeds() throws Exception {
        mvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").isString())
                .andExpect(jsonPath("$.price").isNumber())
                .andExpect(jsonPath("$.servedBy").isString());
    }

    @Test
    @DisplayName("The consumer's own class reads the producer's real answer; extra fields are ignored")
    void consumerCanReadTheAnswer() throws Exception {
        String body = mvc.perform(get("/products/1")).andReturn().getResponse().getContentAsString();

        // the answer has "stock", which CatalogProduct does not have: a tolerant reader skips it
        assertThat(body).contains("\"stock\"");
        CatalogProduct product = json.readValue(body, CatalogProduct.class);

        assertThat(product.id()).isEqualTo(1);
        assertThat(product.name()).isEqualTo("Keyboard");
        assertThat(product.price()).isEqualByComparingTo("49.90");
    }
}
