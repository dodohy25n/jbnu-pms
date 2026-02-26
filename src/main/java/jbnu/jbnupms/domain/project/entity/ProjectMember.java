package jbnu.jbnupms.domain.project.entity;

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
@Table(name = "project_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "project_id", "user_id" })
}, indexes = {
        @Index(name = "idx_project_member_user_id", columnList = "user_id")
})
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ProjectRole role;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Builder
    public ProjectMember(Project project, User user, ProjectRole role) {
        this.project = project;
        this.user = user;
        this.role = role != null ? role : ProjectRole.MEMBER;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void updateRole(ProjectRole role) {
        this.role = role;
    }

    public void updateLastAccessedAt() {
        this.lastAccessedAt = LocalDateTime.now();
    }
}
