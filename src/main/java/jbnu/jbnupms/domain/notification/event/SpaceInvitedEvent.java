package jbnu.jbnupms.domain.notification.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SpaceInvitedEvent {
    private final Long   spaceId;
    private final String spaceName;
    private final Long   invitedUserId;
}