package jbnu.jbnupms.domain.space.entity;

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
@Table(name = "space_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "space_id", "user_id" })
})
public class SpaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SpaceRole role;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Builder
    public SpaceMember(Space space, User user, SpaceRole role) {
        this.space = space;
        this.user = user;
        this.role = role != null ? role : SpaceRole.MEMBER;
        this.joinedAt = LocalDateTime.now();
    }

    public enum SpaceRole {
        ADMIN, MEMBER
    }
}
