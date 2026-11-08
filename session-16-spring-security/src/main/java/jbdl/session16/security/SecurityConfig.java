package jbdl.session16.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * All security rules in one place.
 *
 * AUTHENTICATION (who are you?): HTTP Basic, i.e. every request carries the header
 *   Authorization: Basic base64(username:password)
 * checked against the database (DatabaseUserDetailsService) with BCrypt. Simple for an API and for
 * learning; Session 18 replaces it with OAuth 2 / tokens. Basic auth must only ever be used over HTTPS.
 *
 * AUTHORIZATION (what may you do?): the URL rules below, plus @PreAuthorize on methods
 * (@EnableMethodSecurity switches those on).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
                // URL rules are checked TOP DOWN; the first matching rule wins, so specific rules go first
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                // A stateless API: no server session, every request authenticates itself
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CSRF protection defends BROWSER sessions that send cookies automatically. This API uses
                // no cookies (credentials are sent explicitly on each request), so it is switched off here.
                // Keep it ON for any app with a login form and session cookies.
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /**
     * BCrypt: a deliberately SLOW hash with a random salt built in. Slow makes guessing millions of
     * passwords expensive; the salt makes two users with the same password get different hashes.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
