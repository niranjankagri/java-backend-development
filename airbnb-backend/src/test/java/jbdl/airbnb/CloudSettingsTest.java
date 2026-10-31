package jbdl.airbnb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Session 24: the prod profile configured the way Render configures it (render.yaml): no DATABASE_URL,
 * but DATABASE_HOST, DATABASE_PORT, DATABASE_NAME, DATABASE_USER and DATABASE_PASSWORD separately.
 * application-prod.properties builds the JDBC URL from them.
 *
 * No @ServiceConnection here, on purpose: the connection must come from those five settings alone.
 * (Environment variables and properties are the same thing to Spring, so the test sets them as
 * properties.) Without Docker the class is skipped.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Testcontainers(disabledWithoutDocker = true)
class CloudSettingsTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @DynamicPropertySource
    static void renderStyleSettings(DynamicPropertyRegistry settings) {
        settings.add("DATABASE_HOST", POSTGRES::getHost);
        settings.add("DATABASE_PORT", () -> POSTGRES.getMappedPort(5432));
        settings.add("DATABASE_NAME", POSTGRES::getDatabaseName);
        settings.add("DATABASE_USER", POSTGRES::getUsername);
        settings.add("DATABASE_PASSWORD", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mvc;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Test
    @DisplayName("The JDBC URL is built from host, port and name, and the database is UP")
    void connectsFromParts() throws Exception {
        assertThat(jdbcUrl).isEqualTo("jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432)
                + "/" + POSTGRES.getDatabaseName());

        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.db.status").value("UP"));
    }
}
