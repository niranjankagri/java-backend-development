package jbdl.splitwise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import jbdl.splitwise.expenses.Expense;
import jbdl.splitwise.expenses.ExpenseRepository;
import jbdl.splitwise.expenses.Settlement;
import jbdl.splitwise.expenses.SettlementRepository;
import jbdl.splitwise.expenses.SplitType;
import jbdl.splitwise.groups.ExpenseGroup;
import jbdl.splitwise.groups.ExpenseGroupRepository;
import jbdl.splitwise.users.AppUser;
import jbdl.splitwise.users.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Session 12, topic "database schema and entity design": the tables Hibernate generates from the
 * entities, and that the whole model (user, group, expense with shares, settlement) can be saved and
 * read back. Each test is rolled back (@Transactional), so they don't affect each other.
 */
@SpringBootTest
@Transactional
class SchemaDesignTest {

    @Autowired
    private AppUserRepository users;

    @Autowired
    private ExpenseGroupRepository groups;

    @Autowired
    private ExpenseRepository expenses;

    @Autowired
    private SettlementRepository settlements;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("The schema has the six designed tables (and no table named USER or GROUP)")
    void tables() {
        List<String> tables = jdbc.queryForList(
                "SELECT LOWER(table_name) FROM information_schema.tables WHERE table_schema = 'PUBLIC' ORDER BY 1",
                String.class);

        assertThat(tables).containsExactly(
                "app_user", "expense", "expense_group", "expense_share", "group_member", "settlement");
        // (Session 17 added columns, not tables: password_hash and role on app_user, version on expense_group)
    }

    @Test
    @DisplayName("A full scenario can be stored: users, a group, an expense with shares, a settlement")
    void wholeModel() {
        AppUser asha = users.save(new AppUser("Asha", "asha@example.com", "{noop}not-used-here"));
        AppUser ravi = users.save(new AppUser("Ravi", "ravi@example.com", "{noop}not-used-here"));
        ExpenseGroup goa = new ExpenseGroup("Goa trip", asha);
        goa.addMember(ravi);
        groups.save(goa);

        Expense dinner = new Expense(goa, "Dinner", new BigDecimal("900.00"), asha, SplitType.EQUAL);
        dinner.addShare(asha, new BigDecimal("450.00"));
        dinner.addShare(ravi, new BigDecimal("450.00"));
        expenses.save(dinner); // the shares are saved with it (cascade)
        settlements.save(new Settlement(goa, ravi, asha, new BigDecimal("450.00")));

        // write everything, forget the cached objects, read back from the database
        entityManager.flush();
        entityManager.clear();

        Expense loaded = expenses.findByGroupIdOrderByCreatedAtDesc(goa.getId()).get(0);
        assertThat(loaded.getSplitType()).isEqualTo(SplitType.EQUAL);
        assertThat(loaded.getShares()).hasSize(2);
        assertThat(loaded.getAmount()).isEqualByComparingTo("900.00");
        assertThat(settlements.findByGroupId(goa.getId())).singleElement()
                .satisfies(s -> assertThat(s.getAmount()).isEqualByComparingTo("450.00"));
        assertThat(groups.findById(goa.getId()).orElseThrow().getMembers()).hasSize(2);
    }

    @Test
    @DisplayName("The database refuses a second account with the same email")
    void uniqueEmail() {
        users.save(new AppUser("Asha", "asha@example.com", "{noop}not-used-here"));

        assertThatThrownBy(() -> {
            users.save(new AppUser("Asha again", "asha@example.com", "{noop}not-used-here"));
            entityManager.flush(); // the INSERT is sent here
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("The split type is stored by name, not by position")
    void enumStoredAsText() {
        AppUser asha = users.save(new AppUser("Asha", "asha@example.com", "{noop}not-used-here"));
        ExpenseGroup group = groups.save(new ExpenseGroup("Flat", asha));
        expenses.save(new Expense(group, "Rent", new BigDecimal("20000.00"), asha, SplitType.PERCENT));
        entityManager.flush();

        assertThat(jdbc.queryForObject("SELECT split_type FROM expense", String.class)).isEqualTo("PERCENT");
    }
}
