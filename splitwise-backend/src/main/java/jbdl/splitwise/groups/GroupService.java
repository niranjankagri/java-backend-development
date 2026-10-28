package jbdl.splitwise.groups;

import java.util.List;
import jbdl.splitwise.common.BusinessRuleException;
import jbdl.splitwise.common.NotFoundException;
import jbdl.splitwise.users.AppUser;
import jbdl.splitwise.users.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Rules about groups: who creates them, who can be added, who sees which groups. */
@Service
public class GroupService {

    private final ExpenseGroupRepository groups;
    private final UserService users;

    public GroupService(ExpenseGroupRepository groups, UserService users) {
        this.groups = groups;
        this.users = users;
    }

    @Transactional
    public ExpenseGroup create(String name, long createdByUserId) {
        AppUser creator = users.get(createdByUserId);
        return groups.save(new ExpenseGroup(name.trim(), creator));
    }

    /** Adds a member; adding someone twice is refused, so the client learns about its mistake. */
    @Transactional
    public ExpenseGroup addMember(long groupId, long userId) {
        ExpenseGroup group = get(groupId);
        AppUser user = users.get(userId);
        if (!group.addMember(user)) {
            throw new BusinessRuleException(user.getName() + " is already a member of " + group.getName());
        }
        return group; // no save() needed: the group is managed, the join-table row is written at commit
    }

    @Transactional(readOnly = true)
    public ExpenseGroup get(long groupId) {
        ExpenseGroup group = groups.findById(groupId)
                .orElseThrow(() -> new NotFoundException("group " + groupId + " not found"));
        group.getMembers().size(); // load the lazy member list while the transaction is still open
        return group;
    }

    @Transactional(readOnly = true)
    public List<ExpenseGroup> groupsOf(long userId) {
        users.get(userId); // 404 for an unknown user instead of an empty list
        List<ExpenseGroup> found = groups.findByMembersIdOrderByName(userId);
        for (ExpenseGroup group : found) {
            group.getMembers().size();
        }
        return found;
    }
}
