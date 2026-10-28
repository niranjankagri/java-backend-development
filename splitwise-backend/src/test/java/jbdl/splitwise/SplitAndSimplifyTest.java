package jbdl.splitwise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jbdl.splitwise.expenses.DebtSimplifier;
import jbdl.splitwise.expenses.DebtSimplifier.Payment;
import jbdl.splitwise.expenses.SplitType;
import jbdl.splitwise.expenses.split.SplitStrategies;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Session 13: the money rules as plain unit tests (no Spring, run in milliseconds). Every split must
 * add up to the exact total, paisa for paisa; the simplifier must settle everyone in few payments.
 */
class SplitAndSimplifyTest {

    private static final List<Long> THREE = List.of(1L, 2L, 3L);

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static BigDecimal sum(Map<Long, BigDecimal> shares) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal share : shares.values()) {
            total = total.add(share);
        }
        return total;
    }

    @Test
    @DisplayName("EQUAL: 900.00 among 3 is 300.00 each")
    void equalEven() {
        Map<Long, BigDecimal> shares = SplitStrategies.forType(SplitType.EQUAL).split(money("900.00"), THREE, Map.of());

        assertThat(shares.values()).allMatch(share -> share.compareTo(money("300.00")) == 0);
    }

    @Test
    @DisplayName("EQUAL: 100.00 among 3 is 33.34 + 33.33 + 33.33, never 99.99 or 100.02")
    void equalWithLeftoverPaisa() {
        Map<Long, BigDecimal> shares = SplitStrategies.forType(SplitType.EQUAL).split(money("100.00"), THREE, Map.of());

        assertThat(shares).containsEntry(1L, money("33.34")).containsEntry(2L, money("33.33")).containsEntry(3L, money("33.33"));
        assertThat(sum(shares)).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("EXACT: amounts are kept; a wrong total is refused")
    void exact() {
        Map<Long, BigDecimal> values = Map.of(1L, money("200.00"), 2L, money("300.00"));
        assertThat(SplitStrategies.forType(SplitType.EXACT).split(money("500.00"), List.of(1L, 2L), values))
                .containsEntry(1L, money("200.00")).containsEntry(2L, money("300.00"));

        assertThatThrownBy(() -> SplitStrategies.forType(SplitType.EXACT).split(money("600.00"), List.of(1L, 2L), values))
                .hasMessage("exact amounts add up to 500.00, not 600.00");
    }

    @Test
    @DisplayName("PERCENT: 33.33 % three times of 100.00 still adds up to 100.00 (the last share takes the rest)")
    void percent() {
        Map<Long, BigDecimal> values = Map.of(1L, money("33.34"), 2L, money("33.33"), 3L, money("33.33"));

        Map<Long, BigDecimal> shares = SplitStrategies.forType(SplitType.PERCENT).split(money("100.00"), THREE, values);

        assertThat(sum(shares)).isEqualByComparingTo("100.00");
        assertThatThrownBy(() -> SplitStrategies.forType(SplitType.PERCENT)
                .split(money("100.00"), List.of(1L, 2L), Map.of(1L, money("50"), 2L, money("40"))))
                .hasMessage("percentages add up to 90, not 100");
    }

    @Test
    @DisplayName("Simplify: three balances need only two payments")
    void simplify() {
        Map<Long, BigDecimal> balances = new LinkedHashMap<>();
        balances.put(1L, money("3500.00"));   // Asha should get 3500
        balances.put(2L, money("-1700.00"));  // Ravi must pay 1700
        balances.put(3L, money("-1800.00"));  // Meena must pay 1800

        List<Payment> payments = DebtSimplifier.simplify(balances);

        assertThat(payments).containsExactly(
                new Payment(3L, 1L, money("1800.00")),   // the biggest debtor pays first
                new Payment(2L, 1L, money("1700.00")));
    }

    @Test
    @DisplayName("Simplify: everyone at zero means no payments")
    void nothingToSettle() {
        assertThat(DebtSimplifier.simplify(Map.of(1L, money("0.00"), 2L, money("0.00")))).isEmpty();
    }
}
