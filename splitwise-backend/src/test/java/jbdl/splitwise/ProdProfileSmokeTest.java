package jbdl.splitwise;

import static jbdl.splitwise.ApiTestSupport.as;
import static jbdl.splitwise.ApiTestSupport.createGroup;
import static jbdl.splitwise.ApiTestSupport.register;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jbdl.splitwise.ApiTestSupport.TestUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Session 17, deployment readiness: the app started with the PROD profile against a real PostgreSQL and
 * a real Redis (Testcontainers), the way it runs in Docker. Proves the prod configuration works and
 * the health check reports both dependencies. Skipped on machines without Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@TestPropertySource(properties = "splitwise.admin.password=prod-admin-password-for-test") // normally an env variable
@Testcontainers(disabledWithoutDocker = true)
class ProdProfileSmokeTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Health check: UP, with the PostgreSQL database and Redis both UP")
    void health() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.details.database").value("PostgreSQL"))
                .andExpect(jsonPath("$.components.redis.status").value("UP"));
    }

    @Test
    @DisplayName("The main flow works on PostgreSQL + Redis: register, create a group, read cached balances")
    void mainFlow() throws Exception {
        TestUser asha = register(mvc, "Asha");
        long group = createGroup(mvc, "Prod check", asha);

        mvc.perform(get("/api/groups/" + group + "/balances").with(as(asha)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance").value(0.00));
        mvc.perform(get("/api/groups/" + group + "/balances").with(as(asha))).andExpect(status().isOk()); // from Redis
    }
}
