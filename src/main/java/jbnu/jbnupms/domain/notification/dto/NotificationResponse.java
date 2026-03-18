package jbnu.jbnupms.domain.notification.dto;

import jbnu.jbnupms.domain.notification.entity.Notification;
import jbnu.jbnupms.domain.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {
    private Long             id;
    private NotificationType type;
    private String           title;
    private String           content;
    private String           relatedUrl;
    private boolean          isRead;
    private LocalDateTime    createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id        (n.getId())
                .type      (n.getType())
                .title     (n.getTitle())
                .content   (n.getContent())
                .relatedUrl(n.getRelatedUrl())
                .isRead    (n.isRead())
                .createdAt (n.getCreatedAt())
                .build();
    }
}