package jbdl.airbnb.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Session 22: who may call what (the approach of Sessions 16 and 17).
 * <ul>
 *   <li>open to everyone: register, and BROWSING (search, a listing, a host's listings, reviews),
 *       like on Airbnb, where you log in only to book</li>
 *   <li>everything else needs a login: creating and changing listings, booking, reviewing</li>
 * </ul>
 * WHICH listing or booking a logged-in user may change is decided in the services (403), because only
 * they know who owns it.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        // The rules are checked from the top, and the first match decides. A listing's
                        // bookings are private (its host only), so this rule must come BEFORE the open
                        // GET /api/listings/** below, which would otherwise match it too.
                        .requestMatchers(HttpMethod.GET, "/api/listings/*/bookings").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/listings/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/*/listings").permitAll()
                        .anyRequest().authenticated())
                // HTTP Basic: email and password with every request (always over HTTPS in production)
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable()); // no cookies, so no cross-site request forgery (Session 16)
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
