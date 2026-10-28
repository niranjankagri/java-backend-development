package jbdl.splitwise.groups;

import java.math.BigDecimal;
import jbdl.splitwise.common.BusinessRuleException;
import jbdl.splitwise.expenses.ExpenseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leaving a group touches two features: groups (the member list) and expenses (the balance). This
 * small service combines them, so GroupService and ExpenseService don't need to know each other in
 * both directions (ExpenseService already uses GroupService; the reverse would be a circular dependency).
 */
@Service
public class MembershipService {

    private final GroupService groups;
    private final ExpenseService expenses;

    public MembershipService(GroupService groups, ExpenseService expenses) {
        this.groups = groups;
        this.expenses = expenses;
    }

    /** A member may leave only when they owe nothing and are owed nothing. */
    @Transactional
    public ExpenseGroup leave(long groupId, long userId) {
        BigDecimal balance = expenses.balanceAmounts(groupId).get(userId);
        if (balance != null && balance.signum() != 0) {
            throw new BusinessRuleException("settle the balance of " + balance + " before leaving the group");
        }
        return groups.removeMember(groupId, userId);
    }
}
