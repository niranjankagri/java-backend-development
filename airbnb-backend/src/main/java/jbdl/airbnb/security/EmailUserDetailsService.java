package jbdl.airbnb.security;

import jbdl.airbnb.users.AppUser;
import jbdl.airbnb.users.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Users log in with their EMAIL and password. Spring Security calls this on every request, then
 * compares the password with the stored BCrypt hash. The email is the principal's name afterwards.
 */
@Service
public class EmailUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public EmailUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        AppUser user = users.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("no account for " + email));
        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles("USER")   // one role for everybody: host or guest depends on the listing, not the account
                .build();
    }
}
