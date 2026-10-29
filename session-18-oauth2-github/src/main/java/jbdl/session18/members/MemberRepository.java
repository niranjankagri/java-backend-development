package jbdl.session18.members;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data writes the queries from the method names (Session 11). */
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByGithubId(Long githubId);

    Optional<Member> findByLogin(String login);

    List<Member> findAllByOrderByLastLoginDesc();
}
