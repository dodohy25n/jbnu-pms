package jbnu.jbnupms.domain.task.repository;

import jbnu.jbnupms.domain.task.entity.Task;
import jbnu.jbnupms.domain.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

        // 프로젝트 내 최상위 태스크 조회 (부모가 없는)
        @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.parent IS NULL AND t.deletedAt IS NULL")
        List<Task> findRootTasksByProjectId(@Param("projectId") Long projectId);

        // 프로젝트 내 모든 태스크 조회
        List<Task> findByProjectId(Long projectId);

        // 상태별 조회
        List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);

        // 마감일 범위 내 미완료 태스크 조회 (스케줄러용, project/space fetch join)
        @Query("SELECT t FROM Task t " +
                "JOIN FETCH t.project p " +
                "JOIN FETCH p.space s " +
                "WHERE t.dueDate >= :startDate AND t.dueDate <= :endDate " +
                "AND t.status != :doneStatus")
        List<Task> findTasksDueInRange(
                @Param("startDate") LocalDateTime startDate,
                @Param("endDate") LocalDateTime endDate,
                @Param("doneStatus") TaskStatus doneStatus);

        // 캘린더용: 여러 스페이스의 마감일 범위 내 태스크 조회 (N+1 방지: project/space fetch join)
        @Query("SELECT t FROM Task t " +
                "JOIN FETCH t.project p " +
                "JOIN FETCH p.space s " +
                "WHERE p.space.id IN :spaceIds " +
                "AND t.dueDate >= :startDate AND t.dueDate <= :endDate")
        List<Task> findTasksDueInMonth(
                @Param("spaceIds") List<Long> spaceIds,
                @Param("startDate") LocalDateTime startDate,
                @Param("endDate") LocalDateTime endDate);
}
