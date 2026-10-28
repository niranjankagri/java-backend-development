package jbdl.session16.security;

import jbdl.session16.users.Account;
import jbdl.session16.users.AccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * DATABASE-BASED AUTHENTICATION: the bridge between our accounts table and Spring Security.
 *
 * When someone logs in, Spring Security calls loadUserByUsername with the typed username. We load the
 * account and hand back a UserDetails (username, password HASH, roles, enabled). Spring Security then
 * checks the typed password against the hash with the PasswordEncoder: our code never compares
 * passwords itself.
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AccountRepository accounts;

    public DatabaseUserDetailsService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Account account = accounts.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("no account " + username));
        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .roles(account.getRole().name())      // becomes the authority ROLE_USER / ROLE_ADMIN
                .disabled(!account.isEnabled())       // a disabled account can't log in
                .build();
    }
}
