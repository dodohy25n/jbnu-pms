package jbnu.jbnupms.domain.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jbnu.jbnupms.domain.project.entity.Project;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

        // 스페이스 내 프로젝트 목록 조회
        @Query("SELECT p FROM Project p WHERE p.space.id = :spaceId")
        List<Project> findBySpaceId(Long spaceId);

        // 캘린더용: 여러 스페이스의 마감일 범위 내 프로젝트 조회 (N+1 방지: space fetch join)
        @Query("SELECT p FROM Project p JOIN FETCH p.space s " +
                "WHERE p.space.id IN :spaceIds " +
                "AND p.dueDate >= :startDate AND p.dueDate <= :endDate")
        List<Project> findProjectsDueInMonth(
                @Param("spaceIds") List<Long> spaceIds,
                @Param("startDate") LocalDateTime startDate,
                @Param("endDate") LocalDateTime endDate);
}
