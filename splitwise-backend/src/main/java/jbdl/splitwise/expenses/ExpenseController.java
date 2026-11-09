package jbdl.splitwise.expenses;

import jakarta.validation.Valid;
import java.util.List;
import jbdl.splitwise.expenses.ExpenseDtos.AddExpense;
import jbdl.splitwise.expenses.ExpenseDtos.BalanceView;
import jbdl.splitwise.expenses.ExpenseDtos.ExpenseView;
import jbdl.splitwise.expenses.ExpenseDtos.PaymentView;
import jbdl.splitwise.expenses.ExpenseDtos.RecordSettlement;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Money endpoints of a group: expenses, balances, the settle-up plan and settlements. Session 17: only
 * members of the group (or an admin) may call them; the rule is on every method.
 */
@RestController
@RequestMapping("/api/groups/{groupId}")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @PreAuthorize("@groupAccess.canAccess(#groupId, authentication)")
    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseView addExpense(@PathVariable long groupId, @Valid @RequestBody AddExpense request) {
        return service.addExpense(groupId, request);
    }

    @PreAuthorize("@groupAccess.canAccess(#groupId, authentication)")
    @GetMapping("/expenses")
    public List<ExpenseView> expenses(@PathVariable long groupId) {
        return service.expensesOf(groupId);
    }

    @PreAuthorize("@groupAccess.canAccess(#groupId, authentication)")
    @GetMapping("/balances")
    public List<BalanceView> balances(@PathVariable long groupId) {
        return service.balances(groupId);
    }

    /** Suggested payments only; nothing is recorded until POST /settlements. */
    @PreAuthorize("@groupAccess.canAccess(#groupId, authentication)")
    @GetMapping("/settle-up")
    public List<PaymentView> settleUpPlan(@PathVariable long groupId) {
        return service.settleUpPlan(groupId);
    }

    /** Records a repayment and returns the new balances. */
    @PreAuthorize("@groupAccess.canAccess(#groupId, authentication)")
    @PostMapping("/settlements")
    @ResponseStatus(HttpStatus.CREATED)
    public List<BalanceView> recordSettlement(@PathVariable long groupId, @Valid @RequestBody RecordSettlement request) {
        service.recordSettlement(groupId, request.fromUserId(), request.toUserId(), request.amount());
        return service.balances(groupId); // fresh: the cache entry was evicted when recordSettlement returned
    }
}
