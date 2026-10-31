package jbdl.airbnb.notifications;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Every user's notifications, in memory (gone after a restart; a real app would store them in a table
 * or send them right away). synchronized: listeners and requests run on different threads (Session 5).
 */
@Component
public class NotificationInbox {

    private final Map<Long, List<Notification>> byUser = new HashMap<>();

    public synchronized void add(long userId, String message) {
        // computeIfAbsent: the user's list, created on first use. The longer way:
        //   List<Notification> list = byUser.get(userId);
        //   if (list == null) { list = new ArrayList<>(); byUser.put(userId, list); }
        byUser.computeIfAbsent(userId, id -> new ArrayList<>()).add(new Notification(message, Instant.now()));
    }

    /** A copy of the user's notifications, oldest first. */
    public synchronized List<Notification> of(long userId) {
        return List.copyOf(byUser.getOrDefault(userId, List.of()));
    }
}
