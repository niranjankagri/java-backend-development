package jbdl.session16.notes;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notes belong to their owner. The owner always comes from the LOGGED-IN user (passed in by the
 * controller from the Authentication), never from the request body: a client can't claim to be someone else.
 */
@Service
public class NoteService {

    private final NoteRepository notes;

    public NoteService(NoteRepository notes) {
        this.notes = notes;
    }

    @Transactional
    public Note write(String owner, String text) {
        return notes.save(new Note(owner, text));
    }

    /**
     * Method security with an expression: allowed when the requested username is the logged-in user,
     * or the caller is an ADMIN. "#username" is the method parameter, "authentication" the current
     * login. Otherwise Spring throws AccessDeniedException before the method runs -> HTTP 403.
     *
     * The same check written by hand inside the method would be:
     *   if (!username.equals(currentUser) && !currentUserIsAdmin) throw new AccessDeniedException(...)
     */
    @PreAuthorize("#username == authentication.name or hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<Note> notesOf(String username) {
        return notes.findByOwnerOrderById(username);
    }
}
