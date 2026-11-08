package jbdl.session16.users;

/**
 * What a user is allowed to do. Spring Security names roles with a "ROLE_" prefix inside
 * (ROLE_USER, ROLE_ADMIN); hasRole("ADMIN") in the rules adds that prefix for you.
 */
public enum Role {
    USER,
    ADMIN
}
