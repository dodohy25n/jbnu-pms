package jbnu.jbnupms.domain.task.entity;

import jakarta.persistence.*;
import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tasks", indexes = {
        @Index(name = "idx_task_project_status", columnList = "project_id, status")
})
@SQLDelete(sql = "UPDATE tasks SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Task parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> children = new ArrayList<>();

    private Double progress;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.ORDINAL)
    private TaskStatus status;

    @Enumerated(EnumType.ORDINAL)
    private TaskPriority priority;

    private LocalDateTime dueDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Task(Project project, User creator, Task parent, String title, String description, LocalDateTime dueDate, TaskPriority priority) {
        this.project = project;
        this.creator = creator;
        this.parent = parent;
        this.title = title;
        this.description = description;
        this.status = TaskStatus.NOT_STARTED;
        this.progress = 0.0;
        this.priority = priority != null ? priority : TaskPriority.MEDIUM;
        this.dueDate = dueDate;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String title, String description, TaskStatus status, TaskPriority priority, LocalDateTime dueDate, Double progress) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.progress = progress;
        this.updatedAt = LocalDateTime.now();
    }

    public void addChild(Task child) {
        this.children.add(child);
    }
}
