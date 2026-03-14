package jbnu.jbnupms.domain.task.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskAssigneeRole {
    ASSIGNEE("ASSIGNEE", "담당자"),
    MANAGER("MANAGER", "관리자");

    private final String key;
    private final String description;
}
