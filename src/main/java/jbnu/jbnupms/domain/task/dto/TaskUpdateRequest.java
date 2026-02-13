package jbnu.jbnupms.domain.task.dto;

import jbnu.jbnupms.domain.task.entity.TaskPriority;
import jbnu.jbnupms.domain.task.entity.TaskStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class TaskUpdateRequest {
    
    private String title;
    
    private String description;
    
    private TaskStatus status;
    
    private TaskPriority priority;
    
    private LocalDateTime dueDate;
    
    private Double progress;
}
