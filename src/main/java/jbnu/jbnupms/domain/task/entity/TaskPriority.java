package jbnu.jbnupms.domain.task.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskPriority {
    LOW(0, "낮음"),
    MEDIUM(1, "중간"),
    HIGH(2, "높음");

    private final int value;
    private final String description;
}
