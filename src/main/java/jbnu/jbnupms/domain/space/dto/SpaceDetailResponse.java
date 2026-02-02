package jbnu.jbnupms.domain.space.dto;

import jbnu.jbnupms.domain.space.entity.Space;
import jbnu.jbnupms.domain.space.entity.SpaceMember;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class SpaceDetailResponse {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private LocalDateTime createdAt;
    private List<MemberDto> members;

    public SpaceDetailResponse(Space space, List<SpaceMember> members) {
        this.id = space.getId();
        this.name = space.getName();
        this.description = space.getDescription();
        this.ownerId = space.getOwner().getId();
        this.createdAt = space.getCreatedAt();
        this.members = members.stream()
                .map(MemberDto::new)
                .collect(Collectors.toList());
    }

    @Getter
    public static class MemberDto {
        private Long userId;
        private String userName;
        private String email;
        private String role;

        public MemberDto(SpaceMember spaceMember) {
            this.userId = spaceMember.getUser().getId();
            this.userName = spaceMember.getUser().getName();
            this.email = spaceMember.getUser().getEmail();
            this.role = spaceMember.getRole().name();
        }
    }
}
