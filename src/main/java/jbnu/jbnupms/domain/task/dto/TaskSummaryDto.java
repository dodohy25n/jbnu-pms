package jbnu.jbnupms.domain.task.dto;

import jbnu.jbnupms.domain.task.entity.Task;
import jbnu.jbnupms.domain.task.entity.TaskPriority;
import jbnu.jbnupms.domain.task.entity.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TaskSummaryDto {
    private Long id;
    private String title;
    private Long projectId;
    private String projectName;
    private TaskStatus status;
    private LocalDateTime dueDate;
    private TaskPriority priority;
    private AssigneeSummaryDto assignee;

    public static TaskSummaryDto from(Task task, AssigneeSummaryDto assignee) {
        return TaskSummaryDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .projectId(task.getProject() != null ? task.getProject().getId() : null)
                .projectName(task.getProject() != null ? task.getProject().getName() : null)
                .status(task.getStatus())
                .dueDate(task.getDueDate())
                .priority(task.getPriority())
                .assignee(assignee)
                .build();
    }

    @Getter
    @Builder
    public static class AssigneeSummaryDto {
        private Long userId;
        private String userName;
        private String profileImage;
    }
}
