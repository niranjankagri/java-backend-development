package jbdl.splitwise;

import static jbdl.splitwise.ApiTestSupport.as;
import static jbdl.splitwise.ApiTestSupport.createGroup;
import static jbdl.splitwise.ApiTestSupport.register;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jbdl.splitwise.ApiTestSupport.TestUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Session 17: Spring Security and role-based access on the Splitwise API.
 *   no login -> 401;  logged in but not a member of the group -> 403;  member -> 200;  ADMIN -> any group.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    private static final String ADMIN_EMAIL = "admin@splitwise.local";     // application.properties (dev values)
    private static final String ADMIN_PASSWORD = "admin-password-dev";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Without a login every API call is 401; the health check stays open")
    void loginRequired() throws Exception {
        mvc.perform(get("/api/groups/1/balances")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Only members see a group: a stranger gets 403 for the group, its balances and its expenses")
    void membersOnly() throws Exception {
        TestUser asha = register(mvc, "Asha");
        TestUser stranger = register(mvc, "Stranger");
        long group = createGroup(mvc, "Private trip", asha);

        mvc.perform(get("/api/groups/" + group).with(as(asha))).andExpect(status().isOk());
        mvc.perform(get("/api/groups/" + group).with(as(stranger))).andExpect(status().isForbidden());
        mvc.perform(get("/api/groups/" + group + "/balances").with(as(stranger))).andExpect(status().isForbidden());
        mvc.perform(get("/api/groups/" + group + "/expenses").with(as(stranger))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unknown group: 403 for a user (doesn't reveal whether it exists), 404 for an admin")
    void unknownGroup() throws Exception {
        TestUser asha = register(mvc, "Asha");

        mvc.perform(get("/api/groups/999999").with(as(asha))).andExpect(status().isForbidden());
        mvc.perform(get("/api/groups/999999").with(httpBasic(ADMIN_EMAIL, ADMIN_PASSWORD))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A user lists only their own groups")
    void ownGroupList() throws Exception {
        TestUser asha = register(mvc, "Asha");
        TestUser ravi = register(mvc, "Ravi");

        mvc.perform(get("/api/users/" + asha.id() + "/groups").with(as(asha))).andExpect(status().isOk());
        mvc.perform(get("/api/users/" + asha.id() + "/groups").with(as(ravi))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN role: the admin endpoint and any group; a USER gets 403 on /api/admin")
    void adminRole() throws Exception {
        TestUser asha = register(mvc, "Asha");
        long group = createGroup(mvc, "Asha's group", asha);

        mvc.perform(get("/api/admin/users").with(as(asha))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").with(httpBasic(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem(asha.email())))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/groups/" + group).with(httpBasic(ADMIN_EMAIL, ADMIN_PASSWORD))).andExpect(status().isOk());
    }
}
