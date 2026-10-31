package jbdl.airbnb.users;

import jbdl.airbnb.common.BusinessRuleException;
import jbdl.airbnb.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Rules about users: an email can register only once. */
@Service
public class UserService {

    private final AppUserRepository users;

    public UserService(AppUserRepository users) {
        this.users = users;
    }

    @Transactional
    public AppUser register(String name, String email) {
        // "Asha@Example.com" and "asha@example.com" must be the same account
        String normalisedEmail = email.trim().toLowerCase();
        if (users.existsByEmail(normalisedEmail)) {
            throw new BusinessRuleException("email " + normalisedEmail + " is already registered");
        }
        return users.save(new AppUser(name.trim(), normalisedEmail));
    }

    @Transactional(readOnly = true)
    public AppUser get(long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("user " + id + " not found"));
    }
}
