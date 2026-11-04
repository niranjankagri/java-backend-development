package jbdl.splitwise.expenses;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Used from Session 13, when expenses are added. */
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByGroupIdOrderByCreatedAtDesc(Long groupId);
}
