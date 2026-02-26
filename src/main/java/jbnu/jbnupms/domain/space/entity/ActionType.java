package jbnu.jbnupms.domain.space.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActionType {
    TASK_COMPLETED, // 작업 완료
    TASK_DUE_DATE_CHANGED, // 마감일 변경
    COMMENT_ADDED, // 댓글 추가
    ASSIGNEE_CHANGED // 담당자 변경
}
