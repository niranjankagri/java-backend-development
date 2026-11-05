package jbdl.splitwise;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Session 13 end to end, through the HTTP API, with the Goa trip from the requirements:
 *   Hotel 6000.00 paid by Asha, split equally among Asha, Ravi, Meena  -> 2000 each
 *   Dinner 900.00 paid by Ravi, split equally                          -> 300 each
 *   Taxi   500.00 paid by Meena, exact: Asha 200, Ravi 300
 * Expected balances (paid - owed): Asha +3500, Ravi -1700, Meena -1800.
 * Each test builds its own group with new users, so tests never share data.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExpenseFlowApiTest {

    @Autowired
    private MockMvc mvc;

    private long asha;
    private long ravi;
    private long meena;
    private long goa;

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long idOf(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long user(String name) throws Exception {
        String email = name.toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        return idOf(postJson("/api/users", "{\"name\": \"" + name + "\", \"email\": \"" + email + "\"}"));
    }

    private ResultActions expense(String description, String amount, long paidBy, String splitType, String participants)
            throws Exception {
        return postJson("/api/groups/" + goa + "/expenses", "{\"description\": \"" + description + "\", \"amount\": "
                + amount + ", \"paidByUserId\": " + paidBy + ", \"splitType\": \"" + splitType
                + "\", \"participants\": " + participants + "}");
    }

    private String everyone() {
        return "[{\"userId\": " + asha + "}, {\"userId\": " + ravi + "}, {\"userId\": " + meena + "}]";
    }

    /** Three users in one new group, and the three expenses of the trip. */
    @BeforeEach
    void goaTrip() throws Exception {
        asha = user("Asha");
        ravi = user("Ravi");
        meena = user("Meena");
        goa = idOf(postJson("/api/groups", "{\"name\": \"Goa trip\", \"createdByUserId\": " + asha + "}"));
        postJson("/api/groups/" + goa + "/members", "{\"userId\": " + ravi + "}").andExpect(status().isOk());
        postJson("/api/groups/" + goa + "/members", "{\"userId\": " + meena + "}").andExpect(status().isOk());

        expense("Hotel", "6000.00", asha, "EQUAL", everyone()).andExpect(status().isCreated());
        expense("Dinner", "900.00", ravi, "EQUAL", everyone()).andExpect(status().isCreated());
        expense("Taxi", "500.00", meena, "EXACT",
                "[{\"userId\": " + asha + ", \"value\": 200.00}, {\"userId\": " + ravi + ", \"value\": 300.00}]")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shares", hasSize(2)));
    }

    @Test
    @DisplayName("Balances: Asha +3500, Ravi -1700, Meena -1800 (they add up to zero)")
    void balances() throws Exception {
        mvc.perform(get("/api/groups/" + goa + "/balances"))
                .andExpect(jsonPath("$[0].name").value("Asha"))
                .andExpect(jsonPath("$[0].balance").value(3500.00))
                .andExpect(jsonPath("$[1].balance").value(-1700.00))
                .andExpect(jsonPath("$[2].balance").value(-1800.00));
    }

    @Test
    @DisplayName("Settle-up plan: two payments clear the trip")
    void settleUpPlan() throws Exception {
        mvc.perform(get("/api/groups/" + goa + "/settle-up"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].from").value("Meena"))
                .andExpect(jsonPath("$[0].to").value("Asha"))
                .andExpect(jsonPath("$[0].amount").value(1800.00))
                .andExpect(jsonPath("$[1].from").value("Ravi"))
                .andExpect(jsonPath("$[1].amount").value(1700.00));
    }

    @Test
    @DisplayName("Settlement flow: Ravi pays Asha 1700 -> Ravi is at zero; overpaying is refused")
    void settlement() throws Exception {
        postJson("/api/groups/" + goa + "/settlements",
                "{\"fromUserId\": " + ravi + ", \"toUserId\": " + asha + ", \"amount\": 1700.00}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].balance").value(1800.00))   // Asha still gets 1800 from Meena
                .andExpect(jsonPath("$[1].balance").value(0.00));     // Ravi is done

        postJson("/api/groups/" + goa + "/settlements",
                "{\"fromUserId\": " + meena + ", \"toUserId\": " + asha + ", \"amount\": 2000.00}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Meena can pay Asha at most 1800.00"));

        mvc.perform(get("/api/groups/" + goa + "/settle-up")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Expense rules: only members, exact amounts must add up, valid body")
    void expenseRules() throws Exception {
        long stranger = user("Stranger");

        expense("Snacks", "100.00", stranger, "EQUAL", everyone())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("user " + stranger + " is not a member of Goa trip"));
        expense("Boat", "800.00", asha, "EXACT",
                "[{\"userId\": " + asha + ", \"value\": 300.00}, {\"userId\": " + ravi + ", \"value\": 300.00}]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("exact amounts add up to 600.00, not 800.00"));
        expense("", "-5", asha, "EQUAL", "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.participants").exists());
    }

    @Test
    @DisplayName("Expense list: newest first, with each share")
    void expenseList() throws Exception {
        mvc.perform(get("/api/groups/" + goa + "/expenses"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].description").value("Taxi"))
                .andExpect(jsonPath("$[2].shares", hasSize(3)));
    }

    @Test
    @DisplayName("Group management: rename; leaving needs a zero balance; the creator can't leave")
    void groupManagement() throws Exception {
        mvc.perform(patch("/api/groups/" + goa).contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Goa 2026\"}"))
                .andExpect(jsonPath("$.name").value("Goa 2026"));

        mvc.perform(delete("/api/groups/" + goa + "/members/" + meena))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("settle the balance of -1800.00 before leaving the group"));

        postJson("/api/groups/" + goa + "/settlements",
                "{\"fromUserId\": " + ravi + ", \"toUserId\": " + asha + ", \"amount\": 1700.00}");
        mvc.perform(delete("/api/groups/" + goa + "/members/" + ravi))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(2)));

        mvc.perform(delete("/api/groups/" + goa + "/members/" + asha)).andExpect(status().isConflict());
    }
}
