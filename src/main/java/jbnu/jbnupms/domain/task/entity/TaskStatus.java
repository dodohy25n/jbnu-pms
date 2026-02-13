package jbnu.jbnupms.domain.task.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskStatus {
    NOT_STARTED(0, "시작 전"),
    IN_PROGRESS(1, "진행 중"),
    DONE(2, "완료");

    private final int value;
    private final String description;
}
