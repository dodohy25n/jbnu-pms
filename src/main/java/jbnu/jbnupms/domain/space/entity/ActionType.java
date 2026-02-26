package jbnu.jbnupms.domain.space.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActionType {
    TASK_COMPLETED,    // 작업 완료
    TASK_DUE_REACHED,  // 마감 도달 (당일)
    TASK_OVERDUE,      // 마감 지연 (초과)
    COMMENT_ADDED,     // 댓글 추가
    ASSIGNEE_CHANGED   // 담당자 변경
}
