package jbdl.splitwise.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What goes in and out of the user API. Entities are never sent as JSON directly: the API shape stays
 * stable when the database model changes, and nothing internal leaks out (Session 8, topic 4).
 */
public final class UserDtos {

    private UserDtos() {
    }

    /** POST /api/users body. */
    public record CreateUser(@NotBlank @Size(max = 100) String name, @NotBlank @Email String email,
                             @NotBlank @Size(min = 12, max = 100) String password) {
    }

    /** A user in responses. */
    public record UserView(long id, String name, String email) {

        public static UserView of(AppUser user) {
            return new UserView(user.getId(), user.getName(), user.getEmail());
        }
    }
}
