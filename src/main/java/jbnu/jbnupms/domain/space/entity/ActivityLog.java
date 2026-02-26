package jbnu.jbnupms.domain.space.entity;

import jbnu.jbnupms.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column
    private Long projectId;

    @Column
    private String projectTitle;

    @Column
    private Long taskId;

    @Column
    private String taskTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = true)
    private User actor;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ActivityLog(Space space, Long projectId, String projectTitle, Long taskId, String taskTitle,
            ActionType actionType, User actor, String summary) {
        this.space = space;
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.actionType = actionType;
        this.actor = actor;
        this.summary = summary;
        this.createdAt = LocalDateTime.now();
    }
}
