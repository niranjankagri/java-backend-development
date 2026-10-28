package jbdl.splitwise.groups;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseGroupRepository extends JpaRepository<ExpenseGroup, Long> {

    /** Every group the user is a member of ("Members" = the members field, "Id" = its id). */
    List<ExpenseGroup> findByMembersIdOrderByName(Long userId);

    /** Is the user with this email a member of the group? Used by the security rules (GroupAccess). */
    boolean existsByIdAndMembersEmail(Long groupId, String email);
}
