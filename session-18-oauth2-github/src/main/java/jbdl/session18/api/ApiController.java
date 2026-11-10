package jbdl.session18.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jbdl.session18.github.GitHubApiException;
import jbdl.session18.github.GitHubClient;
import jbdl.session18.github.RepoSummary;
import jbdl.session18.members.Member;
import jbdl.session18.members.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The API: public, for any logged-in member, and for admins. */
@RestController
public class ApiController {

    private final MemberService memberService;
    private final GitHubClient gitHubClient;

    public ApiController(MemberService memberService, GitHubClient gitHubClient) {
        this.memberService = memberService;
        this.gitHubClient = gitHubClient;
    }

    @GetMapping("/api/public/hello")
    public String hello() {
        return "Hello, anyone can read this";
    }

    /** Who am I? The GitHub profile Spring Security stored at login, plus my roles. */
    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal OAuth2User user) {
        List<String> roles = new ArrayList<>();
        for (GrantedAuthority authority : user.getAuthorities()) {
            if (authority.getAuthority().startsWith("ROLE_")) {
                roles.add(authority.getAuthority());
            }
        }
        return new MeResponse(user.getName(), user.getAttribute("name"), user.getAttribute("avatar_url"), roles);
    }

    /**
     * My recent GitHub repositories, read with MY access token.
     * @RegisteredOAuth2AuthorizedClient("github") hands over what Spring Security saved at login for this
     * user and the "github" registration: above all, the access token.
     */
    @GetMapping("/api/github/repos")
    public List<RepoSummary> myRepos(@RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient gitHub) {
        return gitHubClient.recentRepos(gitHub.getAccessToken().getTokenValue());
    }

    /** Admins only (see SecurityConfig): everyone who has logged in so far. */
    @GetMapping("/api/admin/members")
    public List<MemberResponse> members() {
        List<MemberResponse> result = new ArrayList<>();
        for (Member m : memberService.allMembers()) {
            result.add(new MemberResponse(m.getLogin(), m.getName(), m.getRole().name(), m.getLoginCount(), String.valueOf(m.getLastLogin())));
        }
        return result;
    }

    /** A GitHub error becomes 502 Bad Gateway ("the service behind me failed") with a readable message. */
    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<Map<String, Object>> gitHubFailed(GitHubApiException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage(), "githubStatus", e.getGitHubStatus()));
    }

    /** What /api/me returns. name and avatarUrl may be null (both are optional on GitHub). */
    public record MeResponse(String login, String name, String avatarUrl, List<String> roles) {
    }

    /** One member for the admin list; never the whole entity. */
    public record MemberResponse(String login, String name, String role, int logins, String lastLogin) {
    }
}
