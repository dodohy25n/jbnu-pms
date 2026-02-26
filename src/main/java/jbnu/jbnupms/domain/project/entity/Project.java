package jbnu.jbnupms.domain.project.entity;

import jakarta.persistence.*;
import jbnu.jbnupms.domain.space.entity.Space;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "projects", indexes = {
        @Index(name = "idx_project_space_id", columnList = "space_id")
})
@SQLDelete(sql = "UPDATE projects SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double progress;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.NOT_STARTED;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Project(Space space, String name, String description, LocalDateTime dueDate, Boolean isPublic) {
        this.space = space;
        this.name = name;
        this.description = description;
        this.dueDate = dueDate;
        this.isPublic = (isPublic != null) ? isPublic : true;
        this.progress = 0.0;
        this.status = ProjectStatus.NOT_STARTED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String name, String description, LocalDateTime dueDate, Boolean isPublic, ProjectStatus status) {
        this.name = name;
        this.description = description;
        if (dueDate != null)
            this.dueDate = dueDate;
        if (isPublic != null)
            this.isPublic = isPublic;
        if (status != null)
            this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}
