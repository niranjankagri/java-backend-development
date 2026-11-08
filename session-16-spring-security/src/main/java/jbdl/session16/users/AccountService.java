package jbdl.session16.users;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration and account administration. */
@Service
public class AccountService {

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    /** New users are always plain USERs; the password is hashed before it is stored. */
    @Transactional
    public Account register(String username, String rawPassword) {
        if (accounts.existsByUsername(username)) {
            throw new IllegalStateException("username " + username + " is taken");
        }
        return accounts.save(new Account(username, passwordEncoder.encode(rawPassword), Role.USER));
    }

    /** Method security: even if a URL rule were forgotten, only an ADMIN can run this. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<String> allUsernames() {
        List<String> names = new ArrayList<>();
        for (Account account : accounts.findAll()) {
            names.add(account.getUsername() + " (" + account.getRole() + ")");
        }
        return names;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void disable(String username) {
        accounts.findByUsername(username).orElseThrow().disable();
    }
}
