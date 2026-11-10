package jbdl.session18.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jbdl.session18.members.Member;
import jbdl.session18.members.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * The last step of "Log in with GitHub": turning the GitHub user into a user of THIS app.
 *
 * By the time Spring Security calls loadUser(...), it has already exchanged the one-time code for an
 * access token. The default service then calls GitHub's API (GET https://api.github.com/user) with that
 * token and gets the profile as a map: id, login, name, avatar_url, ...
 *
 * This class adds two things the default does not know about:
 * 1. a local Member record (created on the first login, updated after that), and
 * 2. the member's role as an authority (ROLE_USER or ROLE_ADMIN), so URL rules like
 *    hasRole("ADMIN") work exactly as with password logins in Session 16.
 */
@Service
public class GitHubMemberService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> gitHubProfiles;
    private final MemberService memberService;

    /**
     * Used by Spring: the real DefaultOAuth2UserService asks GitHub for the profile.
     * @Autowired tells Spring which of the two constructors to use (with one constructor it is not needed).
     */
    @Autowired
    public GitHubMemberService(MemberService memberService) {
        this(new DefaultOAuth2UserService(), memberService);
    }

    /** Used by the tests: a fake profile source, so no request goes to GitHub. */
    GitHubMemberService(OAuth2UserService<OAuth2UserRequest, OAuth2User> gitHubProfiles, MemberService memberService) {
        this.gitHubProfiles = gitHubProfiles;
        this.memberService = memberService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User gitHubUser = gitHubProfiles.loadUser(request);
        return toAppUser(gitHubUser);
    }

    /** GitHub profile in, user of this app (with a role) out. */
    OAuth2User toAppUser(OAuth2User gitHubUser) {
        Map<String, Object> profile = gitHubUser.getAttributes();
        // GitHub sends the id as a JSON number; Number covers both Integer and Long.
        Long githubId = ((Number) profile.get("id")).longValue();
        String login = (String) profile.get("login");
        String name = (String) profile.get("name"); // may be null: the name is optional on GitHub
        String avatarUrl = (String) profile.get("avatar_url");

        Member member = memberService.recordLogin(githubId, login, name, avatarUrl);

        // Keep the authorities GitHub's login gave (like OAUTH2_USER and SCOPE_read:user) and add the role.
        List<GrantedAuthority> authorities = new ArrayList<>(gitHubUser.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));

        // "login" names the attribute that getName() returns, so the principal's name is the GitHub login.
        return new DefaultOAuth2User(authorities, profile, "login");
    }
}
