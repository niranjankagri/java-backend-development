package jbdl.session16.users;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Two demo accounts so the app can be tried at once: admin / admin-pass-123 (ADMIN) and
 * asha / asha-pass-123 (USER). Passwords are hashed at startup like any other. A real application
 * never has default passwords in its code: the first admin is created by a setup step.
 */
@Component
public class StartupAccounts implements ApplicationRunner {

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public StartupAccounts(AccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!accounts.existsByUsername("admin")) {
            accounts.save(new Account("admin", passwordEncoder.encode("admin-pass-123"), Role.ADMIN));
        }
        if (!accounts.existsByUsername("asha")) {
            accounts.save(new Account("asha", passwordEncoder.encode("asha-pass-123"), Role.USER));
        }
    }
}
