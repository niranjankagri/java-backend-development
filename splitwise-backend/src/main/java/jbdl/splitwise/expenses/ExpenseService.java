package jbdl.splitwise.expenses;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jbdl.splitwise.common.BusinessRuleException;
import jbdl.splitwise.expenses.DebtSimplifier.Payment;
import jbdl.splitwise.expenses.ExpenseDtos.AddExpense;
import jbdl.splitwise.expenses.ExpenseDtos.BalanceView;
import jbdl.splitwise.expenses.ExpenseDtos.ExpenseView;
import jbdl.splitwise.expenses.ExpenseDtos.Participant;
import jbdl.splitwise.expenses.ExpenseDtos.PaymentView;
import jbdl.splitwise.expenses.split.SplitStrategies;
import jbdl.splitwise.groups.ExpenseGroup;
import jbdl.splitwise.groups.GroupService;
import jbdl.splitwise.users.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The money flows of a group: add an expense, compute balances, suggest repayments, record a
 * settlement. Every rule that protects the numbers lives here.
 */
@Service
public class ExpenseService {

    private final GroupService groups;
    private final ExpenseRepository expenses;
    private final SettlementRepository settlements;

    public ExpenseService(GroupService groups, ExpenseRepository expenses, SettlementRepository settlements) {
        this.groups = groups;
        this.expenses = expenses;
        this.settlements = settlements;
    }

    // ---------- Expense creation flow ----------

    /**
     * 1. load the group (404 if unknown)   2. payer and participants must be members
     * 3. compute the shares with the strategy for the split type   4. save expense + shares (cascade)
     */
    @Transactional
    public ExpenseView addExpense(long groupId, AddExpense request) {
        ExpenseGroup group = groups.get(groupId);
        AppUser payer = member(group, request.paidByUserId());

        List<Long> participantIds = new ArrayList<>();
        Map<Long, BigDecimal> values = new HashMap<>();
        for (Participant participant : request.participants()) {
            if (participantIds.contains(participant.userId())) {
                throw new BusinessRuleException("user " + participant.userId() + " is listed twice");
            }
            member(group, participant.userId()); // must be in the group
            participantIds.add(participant.userId());
            if (participant.value() != null) {
                values.put(participant.userId(), participant.value());
            }
        }

        Map<Long, BigDecimal> shares;
        try {
            shares = SplitStrategies.forType(request.splitType()).split(request.amount(), participantIds, values);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(e.getMessage()); // e.g. "exact amounts add up to 800.00, not 900.00"
        }

        Expense expense = new Expense(group, request.description().trim(), request.amount(), payer, request.splitType());
        for (Map.Entry<Long, BigDecimal> share : shares.entrySet()) {
            expense.addShare(member(group, share.getKey()), share.getValue());
        }
        return ExpenseView.of(expenses.save(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseView> expensesOf(long groupId) {
        groups.get(groupId);
        List<ExpenseView> views = new ArrayList<>();
        for (Expense expense : expenses.findByGroupIdOrderByCreatedAtDesc(groupId)) {
            views.add(ExpenseView.of(expense));
        }
        return views;
    }

    // ---------- Balances ----------

    /**
     * balance = paid for others - own shares + repaid to others - received from others.
     * Every member appears, with 0.00 if they have no activity. The four sums come from the database
     * (one GROUP BY query each), so the cost doesn't grow with the number of expenses loaded into Java.
     */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> balanceAmounts(long groupId) {
        ExpenseGroup group = groups.get(groupId);
        Map<Long, BigDecimal> balance = new LinkedHashMap<>();
        for (AppUser member : group.getMembers()) {
            balance.put(member.getId(), new BigDecimal("0.00"));
        }
        addAll(balance, expenses.totalPaidPerUser(groupId), true);
        addAll(balance, expenses.totalOwedPerUser(groupId), false);
        addAll(balance, settlements.totalRepaidPerUser(groupId), true);
        addAll(balance, settlements.totalReceivedPerUser(groupId), false);
        return balance;
    }

    private static void addAll(Map<Long, BigDecimal> balance, List<UserTotal> totals, boolean add) {
        for (UserTotal total : totals) {
            BigDecimal current = balance.getOrDefault(total.userId(), BigDecimal.ZERO);
            BigDecimal change = add ? total.total() : total.total().negate();
            balance.put(total.userId(), current.add(change));
        }
    }

    @Transactional(readOnly = true)
    public List<BalanceView> balances(long groupId) {
        ExpenseGroup group = groups.get(groupId);
        Map<Long, BigDecimal> amounts = balanceAmounts(groupId);
        List<BalanceView> views = new ArrayList<>();
        for (AppUser member : group.getMembers()) {
            views.add(new BalanceView(member.getId(), member.getName(), amounts.get(member.getId())));
        }
        return views;
    }

    /** The fewest payments that bring every balance back to zero. */
    @Transactional(readOnly = true)
    public List<PaymentView> settleUpPlan(long groupId) {
        ExpenseGroup group = groups.get(groupId);
        Map<Long, String> names = new HashMap<>();
        for (AppUser member : group.getMembers()) {
            names.put(member.getId(), member.getName());
        }
        List<PaymentView> plan = new ArrayList<>();
        for (Payment payment : DebtSimplifier.simplify(balanceAmounts(groupId))) {
            plan.add(new PaymentView(payment.fromUserId(), names.get(payment.fromUserId()),
                    payment.toUserId(), names.get(payment.toUserId()), payment.amount()));
        }
        return plan;
    }

    // ---------- Settlement flow ----------

    /**
     * Records "from paid to". Refused when it would not reduce a real debt: the payer must owe at least
     * that much and the receiver must be owed at least that much (no overpaying, no paying yourself).
     */
    @Transactional
    public List<BalanceView> recordSettlement(long groupId, long fromUserId, long toUserId, BigDecimal amount) {
        ExpenseGroup group = groups.get(groupId);
        AppUser from = member(group, fromUserId);
        AppUser to = member(group, toUserId);
        if (fromUserId == toUserId) {
            throw new BusinessRuleException("a member can't pay themselves");
        }
        Map<Long, BigDecimal> balance = balanceAmounts(groupId);
        BigDecimal owes = balance.get(fromUserId).negate(); // positive if they must pay
        BigDecimal owed = balance.get(toUserId);
        if (amount.compareTo(owes) > 0 || amount.compareTo(owed) > 0) {
            throw new BusinessRuleException(from.getName() + " can pay " + to.getName() + " at most "
                    + owes.min(owed).max(BigDecimal.ZERO));
        }
        settlements.save(new Settlement(group, from, to, amount));
        return balances(groupId);
    }

    /** The user, if they are a member of the group; otherwise a BusinessRuleException (409). */
    private static AppUser member(ExpenseGroup group, long userId) {
        for (AppUser member : group.getMembers()) {
            if (member.getId() == userId) {
                return member;
            }
        }
        throw new BusinessRuleException("user " + userId + " is not a member of " + group.getName());
    }
}
