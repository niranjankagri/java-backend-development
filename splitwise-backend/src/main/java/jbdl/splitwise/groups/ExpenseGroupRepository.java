package jbdl.splitwise.groups;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseGroupRepository extends JpaRepository<ExpenseGroup, Long> {

    /** Every group the user is a member of ("Members" = the members field, "Id" = its id). */
    List<ExpenseGroup> findByMembersIdOrderByName(Long userId);

    /**
     * The group, with its row locked until the transaction ends (SELECT ... FOR UPDATE). Every change that
     * checks a balance and then writes takes this lock first, so two such changes in one group run one
     * after the other and the second one sees what the first one wrote.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from ExpenseGroup g where g.id = :id")
    Optional<ExpenseGroup> lockById(@Param("id") long id);

    /** Is the user with this email a member of the group? Used by the security rules (GroupAccess). */
    boolean existsByIdAndMembersEmail(Long groupId, String email);
}
