package jbnu.jbnupms.domain.project.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectStatus {
    NOT_STARTED,
    IN_PROGRESS,
    DONE
}
