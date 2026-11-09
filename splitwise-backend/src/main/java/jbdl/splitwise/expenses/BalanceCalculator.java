package jbdl.splitwise.expenses;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import jbdl.splitwise.groups.ExpenseGroup;
import jbdl.splitwise.groups.ExpenseGroupRepository;
import jbdl.splitwise.users.AppUser;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Session 17: the balance calculation, CACHED per group (Session 15).
 *
 * Why a separate class? @Cacheable works through a proxy, like @Transactional: if ExpenseService
 * called its own cached method (this.balanceAmounts(...)), the cache would be skipped (the
 * self-invocation trap of Session 9). Calling another bean always goes through the proxy.
 *
 * Every change that can move a balance evicts the group's entry: a new expense, a settlement, a member
 * added or removed (@CacheEvict in ExpenseService and GroupService).
 */
@Component
public class BalanceCalculator {

    public static final String CACHE = "balances";

    private final ExpenseGroupRepository groups;
    private final ExpenseRepository expenses;
    private final SettlementRepository settlements;

    public BalanceCalculator(ExpenseGroupRepository groups, ExpenseRepository expenses, SettlementRepository settlements) {
        this.groups = groups;
        this.expenses = expenses;
        this.settlements = settlements;
    }

    /**
     * balance = paid for others - own shares + repaid to others - received from others (Session 13).
     * Returns a LinkedHashMap (members in order) because the cached value must be Serializable for Redis.
     */
    @Cacheable(value = CACHE, key = "#groupId")
    @Transactional(readOnly = true)
    public LinkedHashMap<Long, BigDecimal> balances(long groupId) {
        ExpenseGroup group = groups.findById(groupId).orElseThrow();
        LinkedHashMap<Long, BigDecimal> balance = new LinkedHashMap<>();
        for (AppUser member : group.getMembers()) {
            balance.put(member.getId(), new BigDecimal("0.00"));
        }
        addAll(balance, expenses.totalPaidPerUser(groupId), true);
        addAll(balance, expenses.totalOwedPerUser(groupId), false);
        addAll(balance, settlements.totalRepaidPerUser(groupId), true);
        addAll(balance, settlements.totalReceivedPerUser(groupId), false);
        return balance;
    }

    private static void addAll(LinkedHashMap<Long, BigDecimal> balance, List<UserTotal> totals, boolean add) {
        for (UserTotal total : totals) {
            BigDecimal current = balance.getOrDefault(total.userId(), BigDecimal.ZERO);
            BigDecimal change = add ? total.total() : total.total().negate();
            balance.put(total.userId(), current.add(change));
        }
    }
}
