package jbnu.jbnupms.domain.calendar.dto;

import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.task.entity.Task;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CalendarItemDto {

    private CalendarItemType type;
    private String title;
    private LocalDateTime dueDate;
    private Long spaceId;
    private String spaceName;
    private Long projectId;
    private String projectName; // TASK 타입일 때만 포함, PROJECT 타입은 null
    private boolean assignedToMe;

    public static CalendarItemDto fromProject(Project project, boolean assignedToMe) {
        return CalendarItemDto.builder()
                .type(CalendarItemType.PROJECT)
                .title(project.getName())
                .dueDate(project.getDueDate())
                .spaceId(project.getSpace().getId())
                .spaceName(project.getSpace().getName())
                .projectId(project.getId())
                .projectName(null)
                .assignedToMe(assignedToMe)
                .build();
    }

    public static CalendarItemDto fromTask(Task task, boolean assignedToMe) {
        return CalendarItemDto.builder()
                .type(CalendarItemType.TASK)
                .title(task.getTitle())
                .dueDate(task.getDueDate())
                .spaceId(task.getProject().getSpace().getId())
                .spaceName(task.getProject().getSpace().getName())
                .projectId(task.getProject().getId())
                .projectName(task.getProject().getName())
                .assignedToMe(assignedToMe)
                .build();
    }
}
