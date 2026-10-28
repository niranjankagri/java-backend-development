package jbdl.splitwise.expenses;

import java.math.BigDecimal;

/**
 * One row of a "sum per user" query, e.g. (userId 1, total 6900.00). JPQL can create it directly:
 *   select new jbdl.splitwise.expenses.UserTotal(e.paidBy.id, sum(e.amount)) from Expense e ... group by e.paidBy.id
 * which is easier to use than the raw Object[] rows such queries return otherwise.
 */
public record UserTotal(Long userId, BigDecimal total) {
}
