package jbdl.splitwise.users;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Support endpoints, ADMIN only (the URL rule /api/admin/** in SecurityConfig). */
@RestController
public class AdminController {

    private final UserService users;

    public AdminController(UserService users) {
        this.users = users;
    }

    /** One line per user: what support staff need, never the password hash. */
    public record UserSummary(long id, String name, String email, Role role) {
    }

    @GetMapping("/api/admin/users")
    public List<UserSummary> allUsers() {
        List<UserSummary> summaries = new ArrayList<>();
        for (AppUser user : users.all()) {
            summaries.add(new UserSummary(user.getId(), user.getName(), user.getEmail(), user.getRole()));
        }
        return summaries;
    }
}
