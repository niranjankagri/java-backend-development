package jbdl.splitwise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import jbdl.splitwise.common.BusinessRuleException;
import jbdl.splitwise.expenses.BalanceCalculator;
import jbdl.splitwise.expenses.Expense;
import jbdl.splitwise.expenses.ExpenseDtos.AddExpense;
import jbdl.splitwise.expenses.ExpenseDtos.ExpenseView;
import jbdl.splitwise.expenses.ExpenseDtos.Participant;
import jbdl.splitwise.expenses.ExpenseRepository;
import jbdl.splitwise.expenses.ExpenseService;
import jbdl.splitwise.expenses.Settlement;
import jbdl.splitwise.expenses.SettlementRepository;
import jbdl.splitwise.expenses.SplitType;
import jbdl.splitwise.groups.ExpenseGroup;
import jbdl.splitwise.groups.GroupService;
import jbdl.splitwise.users.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Session 17: JUnit + Mockito unit tests for ExpenseService (the technique of Session 14). No Spring,
 * no database: the group service, repositories and balance calculator are mocks, so each rule is
 * checked alone and in milliseconds. The API tests prove the parts work together; these prove each rule.
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private GroupService groups;

    @Mock
    private ExpenseRepository expenses;

    @Mock
    private SettlementRepository settlements;

    @Mock
    private BalanceCalculator calculator;

    @InjectMocks // creates ExpenseService with the four mocks above passed to its constructor
    private ExpenseService service;

    private AppUser asha;
    private AppUser ravi;
    private ExpenseGroup goa;

    /** Entities get their ids from the database; in a unit test we set them ourselves. */
    private static AppUser user(long id, String name) {
        AppUser user = new AppUser(name, name.toLowerCase() + "@example.com", "{noop}x");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @BeforeEach
    void goaTrip() {
        asha = user(1, "Asha");
        ravi = user(2, "Ravi");
        goa = new ExpenseGroup("Goa trip", asha);
        goa.addMember(ravi);
        ReflectionTestUtils.setField(goa, "id", 10L);
    }

    @Test
    @DisplayName("addExpense: computes equal shares and saves the expense with them")
    void addExpenseSavesShares() {
        when(groups.get(10)).thenReturn(goa);
        // a real save gives the expense an id; the mock does the same, then returns it
        when(expenses.save(any(Expense.class))).thenAnswer(call -> {
            Expense expense = call.getArgument(0);
            ReflectionTestUtils.setField(expense, "id", 100L);
            return expense;
        });

        ExpenseView view = service.addExpense(10, new AddExpense("Dinner", new BigDecimal("900.00"), 1L, SplitType.EQUAL,
                List.of(new Participant(1L, null), new Participant(2L, null))));

        ArgumentCaptor<Expense> saved = ArgumentCaptor.forClass(Expense.class);
        verify(expenses).save(saved.capture());
        assertThat(saved.getValue().getShares()).hasSize(2);
        assertThat(view.shares()).allSatisfy(share -> assertThat(share.amount()).isEqualByComparingTo("450.00"));
    }

    @Test
    @DisplayName("addExpense: a payer outside the group is refused and nothing is saved")
    void payerMustBeMember() {
        when(groups.get(10)).thenReturn(goa);

        assertThatThrownBy(() -> service.addExpense(10, new AddExpense("Dinner", new BigDecimal("900.00"), 99L,
                SplitType.EQUAL, List.of(new Participant(1L, null)))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user 99 is not a member of Goa trip");
        verify(expenses, never()).save(any());
    }

    @Test
    @DisplayName("addExpense: the same participant twice is refused")
    void noDuplicateParticipants() {
        when(groups.get(10)).thenReturn(goa);

        assertThatThrownBy(() -> service.addExpense(10, new AddExpense("Dinner", new BigDecimal("900.00"), 1L,
                SplitType.EQUAL, List.of(new Participant(2L, null), new Participant(2L, null)))))
                .hasMessage("user 2 is listed twice");
    }

    @Test
    @DisplayName("recordSettlement: a valid repayment is saved")
    void settlementSaved() {
        when(groups.get(10)).thenReturn(goa);
        LinkedHashMap<Long, BigDecimal> balances = new LinkedHashMap<>();
        balances.put(1L, new BigDecimal("450.00"));   // Asha is owed 450
        balances.put(2L, new BigDecimal("-450.00"));  // Ravi owes 450
        when(calculator.balances(10)).thenReturn(balances);

        service.recordSettlement(10, 2, 1, new BigDecimal("450.00"));

        verify(settlements).save(any(Settlement.class));
    }

    @Test
    @DisplayName("recordSettlement: paying more than you owe is refused, nothing saved")
    void overpayRefused() {
        when(groups.get(10)).thenReturn(goa);
        LinkedHashMap<Long, BigDecimal> balances = new LinkedHashMap<>();
        balances.put(1L, new BigDecimal("450.00"));
        balances.put(2L, new BigDecimal("-450.00"));
        when(calculator.balances(10)).thenReturn(balances);

        assertThatThrownBy(() -> service.recordSettlement(10, 2, 1, new BigDecimal("500.00")))
                .hasMessage("Ravi can pay Asha at most 450.00");
        verify(settlements, never()).save(any());
    }
}
