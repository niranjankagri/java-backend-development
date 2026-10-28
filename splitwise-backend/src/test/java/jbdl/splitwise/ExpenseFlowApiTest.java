package jbdl.splitwise;

import static jbdl.splitwise.ApiTestSupport.as;
import static jbdl.splitwise.ApiTestSupport.createGroup;
import static jbdl.splitwise.ApiTestSupport.postJson;
import static jbdl.splitwise.ApiTestSupport.register;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jbdl.splitwise.ApiTestSupport.TestUser;
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
 * Session 13 end to end (updated in Session 17 for login), with the Goa trip from the requirements:
 *   Hotel 6000.00 paid by Asha, split equally among Asha, Ravi, Meena  -> 2000 each
 *   Dinner 900.00 paid by Ravi, split equally                          -> 300 each
 *   Taxi   500.00 paid by Meena, exact: Asha 200, Ravi 300
 * Expected balances (paid - owed): Asha +3500, Ravi -1700, Meena -1800.
 * Session 17 also checks that cached balances are refreshed after every change.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExpenseFlowApiTest {

    @Autowired
    private MockMvc mvc;

    private TestUser asha;
    private TestUser ravi;
    private TestUser meena;
    private long goa;

    private ResultActions expense(String description, String amount, TestUser paidBy, String splitType, String participants)
            throws Exception {
        return postJson(mvc, "/api/groups/" + goa + "/expenses", "{\"description\": \"" + description + "\", \"amount\": "
                + amount + ", \"paidByUserId\": " + paidBy.id() + ", \"splitType\": \"" + splitType
                + "\", \"participants\": " + participants + "}", paidBy);
    }

    private String everyone() {
        return "[{\"userId\": " + asha.id() + "}, {\"userId\": " + ravi.id() + "}, {\"userId\": " + meena.id() + "}]";
    }

    private ResultActions settle(TestUser from, TestUser to, String amount) throws Exception {
        return postJson(mvc, "/api/groups/" + goa + "/settlements",
                "{\"fromUserId\": " + from.id() + ", \"toUserId\": " + to.id() + ", \"amount\": " + amount + "}", from);
    }

    /** Three users in one new group, and the three expenses of the trip. */
    @BeforeEach
    void goaTrip() throws Exception {
        asha = register(mvc, "Asha");
        ravi = register(mvc, "Ravi");
        meena = register(mvc, "Meena");
        goa = createGroup(mvc, "Goa trip", asha);
        postJson(mvc, "/api/groups/" + goa + "/members", "{\"userId\": " + ravi.id() + "}", asha).andExpect(status().isOk());
        postJson(mvc, "/api/groups/" + goa + "/members", "{\"userId\": " + meena.id() + "}", asha).andExpect(status().isOk());

        expense("Hotel", "6000.00", asha, "EQUAL", everyone()).andExpect(status().isCreated());
        expense("Dinner", "900.00", ravi, "EQUAL", everyone()).andExpect(status().isCreated());
        expense("Taxi", "500.00", meena, "EXACT",
                "[{\"userId\": " + asha.id() + ", \"value\": 200.00}, {\"userId\": " + ravi.id() + ", \"value\": 300.00}]")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shares", hasSize(2)));
    }

    @Test
    @DisplayName("Balances: Asha +3500, Ravi -1700, Meena -1800 (they add up to zero)")
    void balances() throws Exception {
        mvc.perform(get("/api/groups/" + goa + "/balances").with(as(ravi)))
                .andExpect(jsonPath("$[0].name").value("Asha"))
                .andExpect(jsonPath("$[0].balance").value(3500.00))
                .andExpect(jsonPath("$[1].balance").value(-1700.00))
                .andExpect(jsonPath("$[2].balance").value(-1800.00));
    }

    @Test
    @DisplayName("Settle-up plan: two payments clear the trip")
    void settleUpPlan() throws Exception {
        mvc.perform(get("/api/groups/" + goa + "/settle-up").with(as(asha)))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].from").value("Meena"))
                .andExpect(jsonPath("$[0].amount").value(1800.00))
                .andExpect(jsonPath("$[1].from").value("Ravi"))
                .andExpect(jsonPath("$[1].amount").value(1700.00));
    }

    @Test
    @DisplayName("Settlement flow: Ravi pays Asha 1700 -> Ravi is at zero; overpaying is refused")
    void settlement() throws Exception {
        settle(ravi, asha, "1700.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].balance").value(1800.00))
                .andExpect(jsonPath("$[1].balance").value(0.00));

        settle(meena, asha, "2000.00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Meena can pay Asha at most 1800.00"));
    }

    @Test
    @DisplayName("Cached balances are refreshed after a new expense and after a settlement (cache eviction)")
    void cacheIsEvictedOnChange() throws Exception {
        // 1. read: the balances are now cached
        mvc.perform(get("/api/groups/" + goa + "/balances").with(as(asha))).andExpect(jsonPath("$[0].balance").value(3500.00));

        // 2. a new expense must invalidate the cached value
        expense("Snacks", "300.00", ravi, "EQUAL", everyone()).andExpect(status().isCreated());
        mvc.perform(get("/api/groups/" + goa + "/balances").with(as(asha)))
                .andExpect(jsonPath("$[0].balance").value(3400.00))    // Asha now owes 100 of the snacks
                .andExpect(jsonPath("$[1].balance").value(-1500.00));  // Ravi paid 300, owes 100

        // 3. so must a settlement
        settle(ravi, asha, "1500.00").andExpect(status().isCreated());
        mvc.perform(get("/api/groups/" + goa + "/balances").with(as(asha))).andExpect(jsonPath("$[1].balance").value(0.00));
    }

    @Test
    @DisplayName("Expense rules: exact amounts must add up, valid body, values must fit the split type")
    void expenseRules() throws Exception {
        expense("Boat", "800.00", asha, "EXACT",
                "[{\"userId\": " + asha.id() + ", \"value\": 300.00}, {\"userId\": " + ravi.id() + ", \"value\": 300.00}]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("exact amounts add up to 600.00, not 800.00"));
        expense("", "-5", asha, "EQUAL", "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.participants").exists());
        // Session 17 custom constraint: EQUAL takes no values, EXACT needs one for everyone
        expense("Tea", "60.00", asha, "EQUAL", "[{\"userId\": " + asha.id() + ", \"value\": 60.00}]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.participants").value("an EQUAL split takes no values"));
        expense("Tea", "60.00", asha, "EXACT", "[{\"userId\": " + asha.id() + "}]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.participants").value("a EXACT split needs a positive value for every participant"));
    }

    @Test
    @DisplayName("Group management: rename; leaving needs a zero balance; the creator can't leave")
    void groupManagement() throws Exception {
        mvc.perform(patch("/api/groups/" + goa).with(as(asha)).contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Goa 2026\"}"))
                .andExpect(jsonPath("$.name").value("Goa 2026"));

        mvc.perform(delete("/api/groups/" + goa + "/members/" + meena.id()).with(as(meena)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("settle the balance of -1800.00 before leaving the group"));

        settle(ravi, asha, "1700.00");
        mvc.perform(delete("/api/groups/" + goa + "/members/" + ravi.id()).with(as(ravi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(2)));

        mvc.perform(delete("/api/groups/" + goa + "/members/" + asha.id()).with(as(asha))).andExpect(status().isConflict());
    }
}
