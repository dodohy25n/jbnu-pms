package jbnu.jbnupms.domain.notification.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CommentCreatedEvent {
    private final Long       taskId;
    private final String     taskTitle;
    private final Long       commentAuthorId;
    private final String     commentAuthorName;
    private final String     commentContent;
    private final List<Long> receiverIds;
}