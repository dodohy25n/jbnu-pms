package jbnu.jbnupms.domain.project.repository;

import jbnu.jbnupms.domain.project.entity.Project;
import jbnu.jbnupms.domain.project.entity.ProjectMember;
import jbnu.jbnupms.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    List<ProjectMember> findByProjectIdIn(List<Long> projectIds);
}
