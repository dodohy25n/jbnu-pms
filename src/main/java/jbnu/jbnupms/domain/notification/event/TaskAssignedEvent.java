package jbnu.jbnupms.domain.notification.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TaskAssignedEvent {
    private final Long   taskId;
    private final String taskTitle;
    private final Long   assigneeId;
    private final String assignerName;
    private final Long   projectId;
}