package jbdl.session18.members;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the local member records in step with GitHub logins.
 *
 * Who is an admin? The GitHub accounts whose numeric ids are listed in the setting app.admin-github-ids
 * (comma separated). Not the logins: a login can be renamed, and the freed name taken by someone else, who
 * would then be an admin. The id never changes. Anyone else who logs in becomes a USER. There is no registration step: logging in with GitHub IS the
 * registration.
 */
@Service
public class MemberService {

    private final MemberRepository members;
    private final Set<Long> adminGithubIds;
    private final Clock clock;

    public MemberService(MemberRepository members, @Value("${app.admin-github-ids:}") Set<Long> adminGithubIds,
            Clock clock) {
        this.members = members;
        this.adminGithubIds = adminGithubIds;
        this.clock = clock;
    }

    /**
     * Called after every successful GitHub login: creates the member the first time, updates the details
     * and the login count after that, and sets the role.
     */
    @Transactional
    public Member recordLogin(Long githubId, String login, String name, String avatarUrl) {
        // Find by the GitHub id; create a new member if this person has never logged in before.
        Member member = members.findByGithubId(githubId).orElse(null);
        if (member == null) {
            member = new Member(githubId, login);
        }
        member.loggedIn(login, name, avatarUrl, Instant.now(clock));
        member.setRole(adminGithubIds.contains(githubId) ? Role.ADMIN : Role.USER);
        return members.save(member);
    }

    /** All members, the most recent login first (for the admin page). */
    @Transactional(readOnly = true)
    public List<Member> allMembers() {
        return members.findAllByOrderByLastLoginDesc();
    }
}
