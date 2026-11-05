package jbdl.splitwise.groups;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import jbdl.splitwise.groups.GroupDtos.AddMember;
import jbdl.splitwise.groups.GroupDtos.CreateGroup;
import jbdl.splitwise.groups.GroupDtos.GroupView;
import jbdl.splitwise.groups.GroupDtos.RenameGroup;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Group endpoints: create a group, add members, read a group, list the groups of a user. */
@RestController
public class GroupController {

    private final GroupService service;
    private final MembershipService membership;

    public GroupController(GroupService service, MembershipService membership) {
        this.service = service;
        this.membership = membership;
    }

    @PostMapping("/api/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupView create(@Valid @RequestBody CreateGroup request) {
        return GroupView.of(service.create(request.name(), request.createdByUserId()));
    }

    @GetMapping("/api/groups/{groupId}")
    public GroupView get(@PathVariable long groupId) {
        return GroupView.of(service.get(groupId));
    }

    @PostMapping("/api/groups/{groupId}/members")
    public GroupView addMember(@PathVariable long groupId, @Valid @RequestBody AddMember request) {
        return GroupView.of(service.addMember(groupId, request.userId()));
    }

    @PatchMapping("/api/groups/{groupId}")
    public GroupView rename(@PathVariable long groupId, @Valid @RequestBody RenameGroup request) {
        return GroupView.of(service.rename(groupId, request.name()));
    }

    /** Leave the group: only with a zero balance; the creator can't leave. */
    @DeleteMapping("/api/groups/{groupId}/members/{userId}")
    public GroupView removeMember(@PathVariable long groupId, @PathVariable long userId) {
        return GroupView.of(membership.leave(groupId, userId));
    }

    /** A nested resource: the groups of one user. */
    @GetMapping("/api/users/{userId}/groups")
    public List<GroupView> groupsOf(@PathVariable long userId) {
        List<GroupView> views = new ArrayList<>();
        for (ExpenseGroup group : service.groupsOf(userId)) {
            views.add(GroupView.of(group));
        }
        return views;
    }
}
