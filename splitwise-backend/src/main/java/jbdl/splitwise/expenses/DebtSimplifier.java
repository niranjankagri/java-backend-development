package jbdl.splitwise.expenses;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns balances into as few payments as possible. A balance is what a person should GET back
 * (positive) or must PAY (negative); all balances of a group add up to zero.
 *
 * Greedy method: take the person who must pay the most and the person who should get the most; the
 * payer pays the smaller of the two amounts; one of them is now settled; repeat. With n people this
 * needs at most n - 1 payments, instead of one payment per expense.
 *
 *   balances: Asha +1700, Ravi -1400, Meena -300
 *   -> Ravi pays Asha 1400 (Ravi done), Meena pays Asha 300 (both done): 2 payments.
 */
public final class DebtSimplifier {

    /** One suggested payment. */
    public record Payment(long fromUserId, long toUserId, BigDecimal amount) {
    }

    /** A person and the amount still open for them (always positive). */
    private static final class OpenAmount {
        private final long userId;
        private BigDecimal amount;

        private OpenAmount(long userId, BigDecimal amount) {
            this.userId = userId;
            this.amount = amount;
        }
    }

    private DebtSimplifier() {
    }

    public static List<Payment> simplify(Map<Long, BigDecimal> balances) {
        List<OpenAmount> toReceive = new ArrayList<>(); // creditors
        List<OpenAmount> toPay = new ArrayList<>();     // debtors
        for (Map.Entry<Long, BigDecimal> entry : balances.entrySet()) {
            if (entry.getValue().signum() > 0) {
                toReceive.add(new OpenAmount(entry.getKey(), entry.getValue()));
            } else if (entry.getValue().signum() < 0) {
                toPay.add(new OpenAmount(entry.getKey(), entry.getValue().negate()));
            }
        }

        List<Payment> payments = new ArrayList<>();
        while (!toReceive.isEmpty() && !toPay.isEmpty()) {
            OpenAmount creditor = largest(toReceive);
            OpenAmount debtor = largest(toPay);

            BigDecimal amount = creditor.amount.min(debtor.amount); // the smaller of the two
            payments.add(new Payment(debtor.userId, creditor.userId, amount));

            creditor.amount = creditor.amount.subtract(amount);
            debtor.amount = debtor.amount.subtract(amount);
            if (creditor.amount.signum() == 0) {
                toReceive.remove(creditor);
            }
            if (debtor.amount.signum() == 0) {
                toPay.remove(debtor);
            }
        }
        return payments;
    }

    /** The entry with the biggest open amount (first one on a tie, so results are repeatable). */
    private static OpenAmount largest(List<OpenAmount> list) {
        OpenAmount best = list.get(0);
        for (OpenAmount candidate : list) {
            if (candidate.amount.compareTo(best.amount) > 0) {
                best = candidate;
            }
        }
        return best;
    }
}
