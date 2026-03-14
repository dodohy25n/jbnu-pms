package jbnu.jbnupms.domain.space.dto;

import jbnu.jbnupms.domain.space.entity.Space;
import jbnu.jbnupms.domain.space.entity.SpaceMember;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class SpaceDetailResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private List<MemberDto> members;

    public static SpaceDetailResponse from(Space space, List<SpaceMember> members) {
        return SpaceDetailResponse.builder()
                .id(space.getId())
                .name(space.getName())
                .description(space.getDescription())
                .createdAt(space.getCreatedAt())
                .members(members.stream()
                        .map(MemberDto::new)
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    public static class MemberDto {
        private Long userId;
        private String userName;
        private String email;
        private String role;
        private String profileImage;

        public MemberDto(SpaceMember spaceMember) {
            this.userId = spaceMember.getUser().getId();
            this.userName = spaceMember.getUser().getName();
            this.email = spaceMember.getUser().getEmail();
            this.role = spaceMember.getRole().name();
            this.profileImage = spaceMember.getUser().getProfileImage();
        }
    }
}