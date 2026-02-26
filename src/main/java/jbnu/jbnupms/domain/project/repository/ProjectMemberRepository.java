package jbnu.jbnupms.domain.project.repository;

import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.project.entity.ProjectMember;
import jbnu.jbnupms.domain.project.entity.ProjectStatus;
import jbnu.jbnupms.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

        boolean existsByProjectIdAndUserId(Long projectId, Long userId);

        boolean existsByProjectAndUser(Project project, User user);

        Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

        @Query("SELECT pm FROM ProjectMember pm JOIN FETCH pm.project p WHERE pm.user.id = :userId AND p.space.id = :spaceId")
        List<ProjectMember> findByUserIdAndSpaceId(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

        List<ProjectMember> findByProjectId(Long projectId);

        @Query("SELECT pm FROM ProjectMember pm " +
                        "JOIN FETCH pm.project p " +
                        "WHERE pm.user.id = :userId " +
                        "AND p.space.id = :spaceId " +
                        "AND p.status = :status " +
                        "ORDER BY pm.lastAccessedAt DESC")
        List<ProjectMember> findTop3RecentProjects(@Param("userId") Long userId, @Param("spaceId") Long spaceId, @Param("status") ProjectStatus status, Pageable pageable);

        List<ProjectMember> findByProjectIdIn(List<Long> projectIds);

        // 캘린더용: 해당 유저가 멤버인 프로젝트 ID 목록에서 일괄 조회 (내 담당 여부 판별)
        @Query("SELECT pm FROM ProjectMember pm WHERE pm.project.id IN :projectIds AND pm.user.id = :userId")
        List<ProjectMember> findByProjectIdInAndUserId(
                @Param("projectIds") List<Long> projectIds,
                @Param("userId") Long userId);
}
