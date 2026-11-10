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
 * Who is an admin? The GitHub logins listed in the setting app.admin-logins (comma separated). Anyone
 * else who logs in becomes a USER. There is no registration step: logging in with GitHub IS the
 * registration.
 */
@Service
public class MemberService {

    private final MemberRepository members;
    private final Set<String> adminLogins;
    private final Clock clock;

    public MemberService(MemberRepository members, @Value("${app.admin-logins:}") Set<String> adminLogins, Clock clock) {
        this.members = members;
        this.adminLogins = adminLogins;
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
        member.setRole(adminLogins.contains(login) ? Role.ADMIN : Role.USER);
        return members.save(member);
    }

    /** All members, the most recent login first (for the admin page). */
    @Transactional(readOnly = true)
    public List<Member> allMembers() {
        return members.findAllByOrderByLastLoginDesc();
    }
}
