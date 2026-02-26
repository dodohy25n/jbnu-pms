package jbnu.jbnupms.domain.project.dto;

import jbnu.jbnupms.domain.project.entity.Project;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import jbnu.jbnupms.domain.project.entity.ProjectMember;
import jbnu.jbnupms.domain.project.entity.ProjectRole;

@Getter
@Builder
public class ProjectResponse {

    private Long id;
    private Long spaceId;
    private String name;
    private String description;
    private Double progress;
    private LocalDateTime dueDate;
    private Boolean isPublic;
    private LocalDateTime updatedAt;
    private String managerName;
    private List<MemberDto> members;

    public static ProjectResponse from(Project project, List<ProjectMember> projectMembers) {
        String managerName = projectMembers.stream()
                .filter(pm -> pm.getRole() == ProjectRole.ADMIN)
                .map(pm -> pm.getUser().getName())
                .findFirst()
                .orElse("Unknown");

        return ProjectResponse.builder()
                .id(project.getId())
                .spaceId(project.getSpace().getId())
                .name(project.getName())
                .description(project.getDescription())
                .progress(project.getProgress())
                .dueDate(project.getDueDate())
                .isPublic(project.getIsPublic())
                .updatedAt(project.getUpdatedAt())
                .managerName(managerName)
                .members(projectMembers.stream()
                        .map(MemberDto::new)
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    public static class MemberDto {
        private Long userId;
        private String userName;
        private String email;
        private String profileImage;
        private String role;

        public MemberDto(ProjectMember projectMember) {
            this.userId = projectMember.getUser().getId();
            this.userName = projectMember.getUser().getName();
            this.email = projectMember.getUser().getEmail();
            this.profileImage = projectMember.getUser().getProfileImage();
            this.role = projectMember.getRole().name();
        }
    }
}
