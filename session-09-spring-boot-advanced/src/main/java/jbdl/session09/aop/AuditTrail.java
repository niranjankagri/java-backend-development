package jbdl.session09.aop;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** Where AuditAspect writes. In production this would be a table or a log shipped elsewhere. */
@Component
public class AuditTrail {

    /** One audit entry: what happened, to what, and whether it succeeded. */
    public record Entry(String action, String detail, boolean success) {
    }

    // many request threads may write at once; reads (the tests) are rare
    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    public void record(Entry entry) {
        entries.add(entry);
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public void clear() {
        entries.clear();
    }
}
