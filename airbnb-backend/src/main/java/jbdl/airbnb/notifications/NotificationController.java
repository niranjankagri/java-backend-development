package jbdl.airbnb.notifications;

import java.security.Principal;
import java.util.List;
import jbdl.airbnb.users.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/users/me/notifications: the logged-in user's notifications. */
@RestController
public class NotificationController {

    private final NotificationInbox inbox;
    private final UserService users;

    public NotificationController(NotificationInbox inbox, UserService users) {
        this.inbox = inbox;
        this.users = users;
    }

    @GetMapping("/api/users/me/notifications")
    public List<Notification> mine(Principal principal) {
        return inbox.of(users.byEmail(principal.getName()).getId());
    }
}
