package jbnu.jbnupms.domain.notification.entity;

import jakarta.persistence.*;
import jbnu.jbnupms.domain.user.entity.User;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_receiver_read", columnList = "receiver_id, is_read"),
        @Index(name = "idx_notification_created_at",   columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 500)
    private String relatedUrl;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Notification(User receiver, NotificationType type,
                        String title, String content, String relatedUrl) {
        this.receiver   = receiver;
        this.type       = type;
        this.title      = title;
        this.content    = content;
        this.relatedUrl = relatedUrl;
        this.isRead     = false;
        this.createdAt  = LocalDateTime.now();
    }

    public void markAsRead() {
        this.isRead = true;
    }
}