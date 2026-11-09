package jbdl.splitwise.groups;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.LinkedHashSet;
import java.util.Set;
import jbdl.splitwise.users.AppUser;

/**
 * A group of people who share expenses: a trip, a flat, a dinner. Called ExpenseGroup (table
 * expense_group) because GROUP is a reserved SQL word (GROUP BY).
 */
@Entity
@Table(name = "expense_group")
public class ExpenseGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Session 17, optimistic locking: Hibernate adds "AND version = ?" to every UPDATE and increases the
     * number. If two people rename the same group at the same moment, the second UPDATE finds a newer
     * version and fails (ObjectOptimisticLockingFailureException) instead of silently overwriting.
     */
    @Version
    private long version;

    /** Who created the group (many groups can be created by one user). */
    @ManyToOne(optional = false)
    private AppUser createdBy;

    /**
     * The members. A user can be in many groups and a group has many users: a join table
     * group_member(group_id, user_id). A Set: no duplicate members.
     *
     * @OrderBy("id"): when Hibernate loads the members from the database it sorts them by user id, i.e.
     * in the order they registered. Without it the order of a loaded Set is random (the first version
     * of the tests failed on exactly that). LinkedHashSet keeps the same order for new, unsaved groups.
     */
    @ManyToMany
    @OrderBy("id")
    @JoinTable(name = "group_member",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<AppUser> members = new LinkedHashSet<>();

    protected ExpenseGroup() {
    }

    /** The creator is always the first member. */
    public ExpenseGroup(String name, AppUser createdBy) {
        this.name = name;
        this.createdBy = createdBy;
        this.members.add(createdBy);
    }

    /** Returns false if the user was already a member (a Set ignores duplicates). */
    public boolean addMember(AppUser user) {
        return members.add(user);
    }

    public boolean removeMember(AppUser user) {
        return members.remove(user);
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public boolean isMember(AppUser user) {
        return members.contains(user);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AppUser getCreatedBy() {
        return createdBy;
    }

    public Set<AppUser> getMembers() {
        return members;
    }
}
