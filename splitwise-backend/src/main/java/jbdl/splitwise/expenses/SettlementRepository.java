package jbdl.splitwise.expenses;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Used from Session 13, when debts are settled. */
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findByGroupId(Long groupId);
}
