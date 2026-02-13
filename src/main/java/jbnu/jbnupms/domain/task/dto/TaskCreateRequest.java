package jbnu.jbnupms.domain.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jbnu.jbnupms.domain.task.entity.TaskPriority;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TaskCreateRequest {
    
    @NotNull(message = "프로젝트 ID는 필수입니다.")
    private Long projectId;
    
    private Long parentId;
    
    @NotBlank(message = "제목은 필수입니다.")
    private String title;
    
    private String description;
    
    private TaskPriority priority;
    
    private LocalDateTime dueDate;
    
    private List<Long> assigneeIds;
}
