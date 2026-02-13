package jbnu.jbnupms.domain.task.entity;

import jakarta.persistence.*;
import jbnu.jbnupms.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "task_assignees", indexes = {
        @Index(name = "idx_task_assignee_task_user", columnList = "task_id, user_id", unique = true),
        @Index(name = "idx_task_assignee_user", columnList = "user_id")
})
public class TaskAssignee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Builder
    public TaskAssignee(Task task, User user) {
        this.task = task;
        this.user = user;
        this.assignedAt = LocalDateTime.now();
    }
}
