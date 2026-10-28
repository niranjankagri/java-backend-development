package jbdl.splitwise.expenses;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * @EntityGraph: load the payer and the shares (with their users) in the SAME query. Without it,
     * showing 20 expenses runs 1 query + 1 per expense for the shares + 1 per share for the user
     * (the N+1 problem of Session 11). The simple version is the same method without the annotation.
     */
    @EntityGraph(attributePaths = {"paidBy", "shares", "shares.user"})
    List<Expense> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    /**
     * How much each member PAID in a group. The database adds the numbers up (SUM ... GROUP BY) and
     * returns one small row per payer, instead of us loading every expense into Java to add them.
     */
    @Query("""
            select new jbdl.splitwise.expenses.UserTotal(e.paidBy.id, sum(e.amount))
            from Expense e
            where e.group.id = :groupId
            group by e.paidBy.id""")
    List<UserTotal> totalPaidPerUser(@Param("groupId") Long groupId);

    /** How much each member OWES in a group: the sum of their shares. */
    @Query("""
            select new jbdl.splitwise.expenses.UserTotal(s.user.id, sum(s.amount))
            from ExpenseShare s
            where s.expense.group.id = :groupId
            group by s.user.id""")
    List<UserTotal> totalOwedPerUser(@Param("groupId") Long groupId);
}
