package jbdl.splitwise.expenses;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Request and response bodies for expenses, balances and settlements. */
public final class ExpenseDtos {

    private ExpenseDtos() {
    }

    /**
     * POST /api/groups/{groupId}/expenses body. `value` of a participant means: nothing for EQUAL,
     * the amount for EXACT, the percentage for PERCENT.
     */
    public record AddExpense(
            @NotBlank @Size(max = 200) String description,
            @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotNull Long paidByUserId,
            @NotNull SplitType splitType,
            @NotEmpty List<@Valid Participant> participants) {
    }

    /** One person sharing an expense. */
    public record Participant(@NotNull Long userId, BigDecimal value) {
    }

    /** POST /api/groups/{groupId}/settlements body: "from paid to this amount". */
    public record RecordSettlement(
            @NotNull Long fromUserId,
            @NotNull Long toUserId,
            @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount) {
    }

    /** An expense in responses. */
    public record ExpenseView(long id, String description, BigDecimal amount, String paidBy, SplitType splitType,
                              List<ShareView> shares) {

        public static ExpenseView of(Expense expense) {
            List<ShareView> shares = new ArrayList<>();
            for (ExpenseShare share : expense.getShares()) {
                shares.add(new ShareView(share.getUser().getId(), share.getUser().getName(), share.getAmount()));
            }
            return new ExpenseView(expense.getId(), expense.getDescription(), expense.getAmount(),
                    expense.getPaidBy().getName(), expense.getSplitType(), shares);
        }
    }

    /** One person's share of an expense. */
    public record ShareView(long userId, String name, BigDecimal amount) {
    }

    /** One member's balance: positive = should get money back, negative = must pay. */
    public record BalanceView(long userId, String name, BigDecimal balance) {
    }

    /** One suggested repayment. */
    public record PaymentView(long fromUserId, String from, long toUserId, String to, BigDecimal amount) {
    }
}
