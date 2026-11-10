package jbdl.session18.members;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.Instant;

/**
 * Someone who has logged in with GitHub at least once.
 *
 * Note what is NOT here: no password. GitHub checks the password; this app only stores who the person
 * is on GitHub. The GitHub user id (a number) is the key, not the login name, because people can
 * rename their GitHub account but the id never changes.
 */
@Entity
public class Member {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private Long githubId;

    @Column(nullable = false)
    private String login;

    private String name;

    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    private Instant firstLogin;

    private Instant lastLogin;

    private int loginCount;

    /** For JPA, which creates objects through a no-argument constructor. */
    protected Member() {
    }

    public Member(Long githubId, String login) {
        this.githubId = githubId;
        this.login = login;
    }

    /** Records one more login and copies the latest details from GitHub (name and picture can change). */
    void loggedIn(String login, String name, String avatarUrl, Instant now) {
        this.login = login;
        this.name = name;
        this.avatarUrl = avatarUrl;
        if (firstLogin == null) {
            firstLogin = now;
        }
        lastLogin = now;
        loginCount++;
    }

    public Long getId() {
        return id;
    }

    public Long getGithubId() {
        return githubId;
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Instant getFirstLogin() {
        return firstLogin;
    }

    public Instant getLastLogin() {
        return lastLogin;
    }

    public int getLoginCount() {
        return loginCount;
    }
}
