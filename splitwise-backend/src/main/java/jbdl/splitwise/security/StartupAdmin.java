package jbdl.splitwise.security;

import jbdl.splitwise.users.AppUser;
import jbdl.splitwise.users.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first admin account once. The password comes from configuration (in production an
 * environment variable SPLITWISE_ADMIN_PASSWORD, see application-prod.properties), never from the code.
 */
@Component
public class StartupAdmin implements ApplicationRunner {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public StartupAdmin(AppUserRepository users, PasswordEncoder passwordEncoder,
                        @Value("${splitwise.admin.email}") String adminEmail,
                        @Value("${splitwise.admin.password}") String adminPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!users.existsByEmail(adminEmail)) {
            AppUser admin = new AppUser("Admin", adminEmail, passwordEncoder.encode(adminPassword));
            admin.makeAdmin();
            users.save(admin);
        }
    }
}
