package jbnu.jbnupms.domain.project.dto;

import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.project.entity.ProjectMember;
import jbnu.jbnupms.domain.project.entity.ProjectStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RecentProjectResponse {
    private Long id;
    private String name;
    private ProjectStatus status;
    private Double progress;
    private LocalDateTime lastAccessedAt;

    public static RecentProjectResponse from(ProjectMember pm) {
        Project project = pm.getProject();
        return RecentProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .status(project.getStatus())
                .progress(project.getProgress())
                .lastAccessedAt(pm.getLastAccessedAt())
                .build();
    }
}
