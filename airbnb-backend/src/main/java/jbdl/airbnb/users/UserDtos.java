package jbdl.airbnb.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** What goes in and out of the user API (entities are never sent as JSON directly). */
public final class UserDtos {

    private UserDtos() {
    }

    /**
     * POST /api/users body. Session 22: with a password, 8 to 72 characters (BCrypt only looks at the
     * first 72 bytes of a password).
     */
    public record CreateUser(@NotBlank @Size(max = 100) String name, @NotBlank @Email String email,
                             @NotBlank @Size(min = 8, max = 72) String password) {
    }

    /** A user in responses. Never with the password hash. */
    public record UserView(long id, String name, String email) {

        public static UserView of(AppUser user) {
            return new UserView(user.getId(), user.getName(), user.getEmail());
        }
    }
}
