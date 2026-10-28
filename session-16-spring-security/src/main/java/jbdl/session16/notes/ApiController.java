package jbdl.session16.notes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import jbdl.session16.users.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * All endpoints in one small controller, so the security rules are easy to compare with the URLs.
 * Spring passes the logged-in user as an Authentication parameter.
 */
@RestController
public class ApiController {

    private final NoteService notes;
    private final AccountService accounts;

    public ApiController(NoteService notes, AccountService accounts) {
        this.notes = notes;
        this.accounts = accounts;
    }

    /** Request bodies. */
    public record Registration(@NotBlank @Size(min = 3, max = 50) String username,
                               @NotBlank @Size(min = 12, max = 100) String password) {
    }

    public record NewNote(@NotBlank @Size(max = 500) String text) {
    }

    /** Response bodies (never the Account entity: it holds the password hash). */
    public record Me(String username, List<String> authorities) {
    }

    public record NoteView(long id, String owner, String text) {
    }

    // ---------- open to everyone ----------

    @GetMapping("/api/public/hello")
    public String hello() {
        return "Hello, anyone can read this";
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Me register(@Valid @RequestBody Registration request) {
        accounts.register(request.username(), request.password());
        return new Me(request.username(), List.of("ROLE_USER"));
    }

    // ---------- any logged-in user ----------

    /** Who am I? Shows what Spring Security knows about the current login. */
    @GetMapping("/api/me")
    public Me me(Authentication authentication) {
        List<String> authorities = new ArrayList<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            authorities.add(authority.getAuthority());
        }
        return new Me(authentication.getName(), authorities);
    }

    @PostMapping("/api/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteView write(@Valid @RequestBody NewNote request, Authentication authentication) {
        Note note = notes.write(authentication.getName(), request.text()); // owner = the logged-in user
        return new NoteView(note.getId(), note.getOwner(), note.getText());
    }

    /** Own notes for everyone; other people's notes only for an ADMIN (see NoteService.notesOf). */
    @GetMapping("/api/notes/of/{username}")
    public List<NoteView> notesOf(@PathVariable String username) {
        List<NoteView> views = new ArrayList<>();
        for (Note note : notes.notesOf(username)) {
            views.add(new NoteView(note.getId(), note.getOwner(), note.getText()));
        }
        return views;
    }

    // ---------- ADMIN only (URL rule /api/admin/** + @PreAuthorize in AccountService) ----------

    @GetMapping("/api/admin/users")
    public List<String> users() {
        return accounts.allUsernames();
    }

    @PostMapping("/api/admin/users/{username}/disable")
    public String disable(@PathVariable String username) {
        accounts.disable(username);
        return username + " disabled";
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail conflict(IllegalStateException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
