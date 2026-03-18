package jbnu.jbnupms.domain.notification.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class TaskDueEvent {
    private final Long       taskId;
    private final String     taskTitle;
    private final List<Long> assigneeIds;
    private final boolean    overdue;
}