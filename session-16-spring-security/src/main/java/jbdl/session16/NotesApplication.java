package jbdl.session16;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A personal notes service protected by Spring Security:
 *   - anyone may read /api/public/** and register
 *   - a logged-in user reads and writes only their own notes
 *   - an ADMIN may also list all users and read anyone's notes
 * Users and their (hashed) passwords are stored in the database.
 */
@SpringBootApplication
public class NotesApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotesApplication.class, args);
    }
}
