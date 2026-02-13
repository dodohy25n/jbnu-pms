package jbnu.jbnupms.domain.task.dto;


import jbnu.jbnupms.domain.task.entity.Task;
import jbnu.jbnupms.domain.task.entity.TaskAssignee;
import jbnu.jbnupms.domain.task.entity.TaskPriority;
import jbnu.jbnupms.domain.task.entity.TaskStatus;
import jbnu.jbnupms.domain.user.dto.UserResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
public class TaskResponse {
    
    private Long id;
    private Long projectId;
    private Long parentId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Double progress;
    private LocalDateTime dueDate;
    private UserResponse creator;
    private List<UserResponse> assignees;
    private List<TaskResponse> children;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    //담당자 정보가 포함된 완전한 응답 객체를 생성할 때 사용
    public static TaskResponse from(Task task, List<TaskAssignee> taskAssignees) {
        return TaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProject().getId())
                .parentId(task.getParent() != null ? task.getParent().getId() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .progress(task.getProgress())
                .dueDate(task.getDueDate())
                .creator(UserResponse.from(task.getCreator()))
                .assignees(taskAssignees != null ? 
                        taskAssignees.stream()
                                .map(ta -> UserResponse.from(ta.getUser()))
                                .collect(Collectors.toList()) : null)
                .children(task.getChildren() != null ?
                        task.getChildren().stream()
                                .map(TaskResponse::from)
                                .collect(Collectors.toList()) : null)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    // 담당자 정보 포함하지 않는 변환
    public static TaskResponse from(Task task) {
         return from(task, Collections.emptyMap());
    }

    public static TaskResponse from(Task task, Map<Long, List<TaskAssignee>> assigneeMap) {
        List<TaskAssignee> taskAssignees = assigneeMap.getOrDefault(task.getId(), Collections.emptyList());

        return TaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProject().getId())
                .parentId(task.getParent() != null ? task.getParent().getId() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .progress(task.getProgress())
                .dueDate(task.getDueDate())
                .creator(UserResponse.from(task.getCreator()))
                .assignees(taskAssignees.stream()
                        .map(ta -> UserResponse.from(ta.getUser()))
                        .collect(Collectors.toList()))
                .children(task.getChildren() != null ?
                        task.getChildren().stream()
                                .map(child -> TaskResponse.from(child, assigneeMap))
                                .collect(Collectors.toList()) : null)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
