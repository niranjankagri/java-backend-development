package jbdl.airbnb.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A person using the app. There is no separate "host" or "guest" account: like on Airbnb, the same
 * person can rent out a flat and book a stay elsewhere. A user is a host because they own listings.
 * The table is app_user because USER is a reserved word in SQL (as in Splitwise).
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150) // one account per email address; also the login name
    private String email;

    // Session 22: the password, only as a BCrypt hash (never the password itself)
    @Column(nullable = false, length = 100)
    private String passwordHash;

    protected AppUser() {
        // for JPA
    }

    public AppUser(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
