package jbdl.splitwise.expenses;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findByGroupId(Long groupId);

    /** How much each member has paid back to others in a group. */
    @Query("""
            select new jbdl.splitwise.expenses.UserTotal(s.fromUser.id, sum(s.amount))
            from Settlement s
            where s.group.id = :groupId
            group by s.fromUser.id""")
    List<UserTotal> totalRepaidPerUser(@Param("groupId") Long groupId);

    /** How much each member has received back from others in a group. */
    @Query("""
            select new jbdl.splitwise.expenses.UserTotal(s.toUser.id, sum(s.amount))
            from Settlement s
            where s.group.id = :groupId
            group by s.toUser.id""")
    List<UserTotal> totalReceivedPerUser(@Param("groupId") Long groupId);
}
