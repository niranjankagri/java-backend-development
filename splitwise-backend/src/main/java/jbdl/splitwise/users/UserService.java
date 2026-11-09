package jbdl.splitwise.users;

import java.util.List;
import jbdl.splitwise.common.BusinessRuleException;
import jbdl.splitwise.common.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Rules about users: an email can register only once. */
@Service
public class UserService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /** Registers a USER; the password is stored only as a BCrypt hash (Session 16). */
    @Transactional
    public AppUser register(String name, String email, String rawPassword) {
        // "Asha@Example.com " and "asha@example.com" must be the same account
        String normalisedEmail = email.trim().toLowerCase();
        if (users.existsByEmail(normalisedEmail)) {
            throw new BusinessRuleException("email " + normalisedEmail + " is already registered");
        }
        return users.save(new AppUser(name.trim(), normalisedEmail, passwordEncoder.encode(rawPassword)));
    }

    /** The logged-in user: Spring Security's principal name is the email. */
    @Transactional(readOnly = true)
    public AppUser byEmail(String email) {
        return users.findByEmail(email).orElseThrow(() -> new NotFoundException("user " + email + " not found"));
    }

    /** For the admin endpoint: every user, as name + email + role. */
    @Transactional(readOnly = true)
    public List<AppUser> all() {
        return users.findAll();
    }

    @Transactional(readOnly = true)
    public AppUser get(long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("user " + id + " not found"));
    }
}
