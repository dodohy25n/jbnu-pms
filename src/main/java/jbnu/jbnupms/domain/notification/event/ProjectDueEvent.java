package jbnu.jbnupms.domain.notification.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ProjectDueEvent {
    private final Long       projectId;
    private final String     projectName;
    private final List<Long> memberIds;
    private final boolean    overdue;
}