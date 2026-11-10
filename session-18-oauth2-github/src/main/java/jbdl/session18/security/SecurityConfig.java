package jbdl.session18.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * All security rules in one place.
 *
 * AUTHENTICATION: "Log in with GitHub" (oauth2Login). There are no passwords in this app. The
 * browser is sent to GitHub, the person logs in THERE and agrees to share their profile, and GitHub
 * sends them back with a one-time code that Spring Security swaps for an access token (topic 2).
 * After that the login lives in an ordinary server session (a cookie), like any website login.
 *
 * AUTHORIZATION: the same URL rules as Session 16; the roles come from GitHubMemberService.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain app(HttpSecurity http, GitHubMemberService gitHubMemberService) throws Exception {
        http
                // URL rules are checked TOP DOWN; the first matching rule wins
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers("/", "/index.html", "/error", "/api/public/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // "Log in with GitHub". Spring Boot already knows GitHub's addresses (authorize, token,
                // user info) from the registration id "github" in application.properties.
                .oauth2Login(login -> login
                        // after GitHub: our own step that creates the member and adds the role
                        .userInfoEndpoint(userInfo -> userInfo.userService(gitHubMemberService))
                        // where to go after a successful login
                        .defaultSuccessUrl("/api/me", true))
                // An API caller that is not logged in gets "401 Unauthorized", not a redirect to GitHub:
                // a script or a fetch() call cannot follow a login page. Browsers start the login from
                // the home page link /oauth2/authorization/github instead.
                .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
                .logout(logout -> logout.logoutSuccessUrl("/"));
        // CSRF protection stays ON (the default). Unlike Session 16's stateless API, this app logs people
        // in with a session COOKIE, which the browser sends automatically - exactly the case CSRF
        // protection is for. Logging out is therefore a POST with the CSRF token (Spring Security's /logout page sends it).
        return http.build();
    }
}
