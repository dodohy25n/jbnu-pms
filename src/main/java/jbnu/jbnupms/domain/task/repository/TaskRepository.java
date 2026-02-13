package jbnu.jbnupms.domain.task.repository;

import jbnu.jbnupms.domain.task.entity.Task;
import jbnu.jbnupms.domain.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // 프로젝트 내 최상위 태스크 조회 (부모가 없는)
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.parent IS NULL AND t.deletedAt IS NULL")
    List<Task> findRootTasksByProjectId(@Param("projectId") Long projectId);

    // 프로젝트 내 모든 태스크 조회
    List<Task> findByProjectId(Long projectId);
    
    // 상태별 조회
    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);
}
