package jbdl.airbnb.users;

import jakarta.validation.Valid;
import java.security.Principal;
import jbdl.airbnb.users.UserDtos.CreateUser;
import jbdl.airbnb.users.UserDtos.UserView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** /api/users: register (no login needed) and look up users. Thin: all rules are in UserService. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody CreateUser request) {
        return UserView.of(service.register(request.name(), request.email(), request.password()));
    }

    /**
     * Who am I? Spring passes the logged-in user as a Principal; its name is the email. The exact path
     * /me wins over the pattern /{id}.
     */
    @GetMapping("/me")
    public UserView me(Principal principal) {
        return UserView.of(service.byEmail(principal.getName()));
    }

    @GetMapping("/{id}")
    public UserView get(@PathVariable long id) {
        return UserView.of(service.get(id));
    }
}
