package jbdl.splitwise.groups;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import jbdl.splitwise.users.AppUser;
import jbdl.splitwise.users.UserDtos.UserView;

/** Request and response bodies of the group API. */
public final class GroupDtos {

    private GroupDtos() {
    }

    /** POST /api/groups body: the creator becomes the first member. */
    public record CreateGroup(@NotBlank @Size(max = 100) String name, @NotNull Long createdByUserId) {
    }

    /** POST /api/groups/{id}/members body. */
    public record AddMember(@NotNull Long userId) {
    }

    /** A group in responses, with its members. */
    public record GroupView(long id, String name, UserView createdBy, List<UserView> members) {

        public static GroupView of(ExpenseGroup group) {
            List<UserView> members = new ArrayList<>();
            for (AppUser member : group.getMembers()) {
                members.add(UserView.of(member));
            }
            return new GroupView(group.getId(), group.getName(), UserView.of(group.getCreatedBy()), members);
        }
    }
}
