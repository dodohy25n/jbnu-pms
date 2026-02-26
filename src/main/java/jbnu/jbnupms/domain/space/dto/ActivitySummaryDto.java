package jbnu.jbnupms.domain.space.dto;

import jbnu.jbnupms.domain.space.entity.ActionType;
import jbnu.jbnupms.domain.space.entity.ActivityLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ActivitySummaryDto {
    private Long id;
    private Long projectId;
    private String projectTitle;
    private Long taskId;
    private String taskTitle;
    private ActionType actionType;
    private ActorDto actor;
    private String summary;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class ActorDto {
        private Long id;
        private String name;
        private String profileImage;
    }

    public static ActivitySummaryDto from(ActivityLog activityLog) {
        return ActivitySummaryDto.builder()
                .id(activityLog.getId())
                .projectId(activityLog.getProjectId())
                .projectTitle(activityLog.getProjectTitle())
                .taskId(activityLog.getTaskId())
                .taskTitle(activityLog.getTaskTitle())
                .actionType(activityLog.getActionType())
                .actor(ActorDto.builder()
                        .id(activityLog.getActor().getId())
                        .name(activityLog.getActor().getName())
                        .profileImage(activityLog.getActor().getProfileImage())
                        .build())
                .summary(activityLog.getSummary())
                .createdAt(activityLog.getCreatedAt())
                .build();
    }
}
