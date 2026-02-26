package jbnu.jbnupms.domain.project.dto;

import jbnu.jbnupms.domain.project.entity.ProjectStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ProjectUpdateRequest {

    private String name;
    private String description;
    private LocalDateTime dueDate;
    private Boolean isPublic;
    private ProjectStatus status;
}