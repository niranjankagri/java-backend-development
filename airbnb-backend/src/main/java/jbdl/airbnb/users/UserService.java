package jbdl.airbnb.users;

import jbdl.airbnb.common.BusinessRuleException;
import jbdl.airbnb.common.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Rules about users: an email can register only once; passwords are stored only as hashes. */
@Service
public class UserService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser register(String name, String email, String password) {
        // "Asha@Example.com" and "asha@example.com" must be the same account
        String normalisedEmail = email.trim().toLowerCase();
        if (users.existsByEmail(normalisedEmail)) {
            throw new BusinessRuleException("email " + normalisedEmail + " is already registered");
        }
        // BCrypt (Session 16): a slow hash with a random salt, so the same password gives a different hash
        return users.save(new AppUser(name.trim(), normalisedEmail, passwordEncoder.encode(password)));
    }

    @Transactional(readOnly = true)
    public AppUser get(long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("user " + id + " not found"));
    }

    /**
     * The logged-in user. The login name is the email (see EmailUserDetailsService), so controllers pass
     * principal.getName() here.
     */
    @Transactional(readOnly = true)
    public AppUser byEmail(String email) {
        return users.findByEmail(email).orElseThrow(() -> new NotFoundException("user " + email + " not found"));
    }
}
