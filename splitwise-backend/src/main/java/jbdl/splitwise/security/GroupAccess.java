package jbdl.splitwise.security;

import jbdl.splitwise.groups.ExpenseGroupRepository;
import jbdl.splitwise.users.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * The questions the @PreAuthorize rules ask, as plain Java methods. A bean named "groupAccess" can be
 * called from a rule as @groupAccess.canAccess(#groupId, authentication). Rules stay one line, and
 * the logic is ordinary, testable code.
 */
@Component("groupAccess")
public class GroupAccess {

    private final ExpenseGroupRepository groups;
    private final AppUserRepository users;

    public GroupAccess(ExpenseGroupRepository groups, AppUserRepository users) {
        this.groups = groups;
        this.users = users;
    }

    /** Members of the group, and admins, may see and change a group's data. */
    public boolean canAccess(long groupId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }
        // a group that doesn't exist answers false here; for admins the service still returns 404
        return groups.existsByIdAndMembersEmail(groupId, authentication.getName());
    }

    /** A user may list their own groups (or an admin anyone's). */
    public boolean isSelfOrAdmin(long userId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }
        return users.findById(userId)
                .map(user -> user.getEmail().equals(authentication.getName()))
                .orElse(false);
    }

    private static boolean isAdmin(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                return true;
            }
        }
        return false;
    }
}
