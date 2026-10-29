package jbdl.session18.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import jbdl.session18.members.Member;
import jbdl.session18.members.MemberRepository;
import jbdl.session18.members.MemberService;
import jbdl.session18.members.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * The step after GitHub: turning a GitHub profile into a member of this app with a role.
 *
 * The profile normally comes from GitHub's API (GET /user). Here a fake profile source returns a
 * ready-made profile instead, so the test runs without GitHub but goes through the real
 * GitHubMemberService, MemberService and database.
 */
@SpringBootTest(properties = "app.admin-logins=octo-admin")
class GitHubMemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository members;

    /** What GitHub's /user API returns (shortened) for the given person. */
    private static OAuth2User gitHubProfile(long id, String login, String name) {
        Map<String, Object> profile = Map.of("id", id, "login", login, "name", name, "avatar_url", "https://avatars.example/" + id);
        List<GrantedAuthority> fromGitHubLogin = List.of(new SimpleGrantedAuthority("OAUTH2_USER"), new SimpleGrantedAuthority("SCOPE_read:user"));
        return new DefaultOAuth2User(fromGitHubLogin, profile, "login");
    }

    /** The request Spring Security passes in after the token exchange: the GitHub registration and the token. */
    private static OAuth2UserRequest loginRequest() {
        ClientRegistration gitHub = CommonOAuth2Provider.GITHUB.getBuilder("github").clientId("test-id").clientSecret("test-secret").build();
        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "gho_test", Instant.now(), Instant.now().plusSeconds(3600));
        return new OAuth2UserRequest(gitHub, token);
    }

    @Test
    @DisplayName("First login creates a USER member; the principal's name is the GitHub login")
    void firstLogin() {
        // given: a GitHub profile source that returns octocat's profile.
        // OAuth2UserService has a single method, loadUser(request), so a lambda can stand in for it:
        // "request -> gitHubProfile(...)" is short for a class that implements loadUser and returns that profile.
        GitHubMemberService service = new GitHubMemberService(request -> gitHubProfile(1001, "octocat", "The Octocat"), memberService);

        // when: Spring Security finishes the login
        OAuth2User user = service.loadUser(loginRequest());

        // then: a member exists, and the user carries GitHub's authorities plus ROLE_USER
        Member member = members.findByGithubId(1001L).orElseThrow();
        assertThat(member.getRole()).isEqualTo(Role.USER);
        assertThat(member.getLoginCount()).isEqualTo(1);
        assertThat(user.getName()).isEqualTo("octocat");
        assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("OAUTH2_USER", "SCOPE_read:user", "ROLE_USER");
    }

    @Test
    @DisplayName("A renamed GitHub account is still the same member (the id is the key, not the login)")
    void renamedAccount() {
        // given: someone logged in once as "old-name"
        new GitHubMemberService(request -> gitHubProfile(2002, "old-name", "Asha"), memberService).loadUser(loginRequest());

        // when: they rename their GitHub account and log in again
        new GitHubMemberService(request -> gitHubProfile(2002, "new-name", "Asha K"), memberService).loadUser(loginRequest());

        // then: one member, with the new login and name, and two logins counted
        Member member = members.findByGithubId(2002L).orElseThrow();
        assertThat(member.getLogin()).isEqualTo("new-name");
        assertThat(member.getName()).isEqualTo("Asha K");
        assertThat(member.getLoginCount()).isEqualTo(2);
        assertThat(members.findByLogin("old-name")).isEmpty();
    }

    @Test
    @DisplayName("A login listed in app.admin-logins gets ROLE_ADMIN")
    void adminLogin() {
        OAuth2User user = new GitHubMemberService(request -> gitHubProfile(3003, "octo-admin", "Admin"), memberService)
                .loadUser(loginRequest());

        assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority).contains("ROLE_ADMIN");
        assertThat(members.findByGithubId(3003L).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }
}
