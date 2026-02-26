package jbnu.jbnupms.domain.task.repository;

import jbnu.jbnupms.domain.task.entity.Task;
import jbnu.jbnupms.domain.task.entity.TaskAssignee;
import jbnu.jbnupms.domain.task.entity.TaskStatus;
import jbnu.jbnupms.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, Long> {

    List<TaskAssignee> findByTaskId(Long taskId);

    List<TaskAssignee> findAllByTask_ProjectId(Long projectId);

    Optional<TaskAssignee> findByTaskAndUser(Task task, User user);

    boolean existsByTaskAndUser(Task task, User user);

    void deleteByTaskAndUser(Task task, User user);

    // 오늘까지 마감이거나 이미 지난 내 작업 목록 조회
    @Query("SELECT ta FROM TaskAssignee ta " +
            "JOIN FETCH ta.task t " +
            "JOIN FETCH t.project p " +
            "WHERE ta.user.id = :userId " +
            "AND p.space.id = :spaceId " +
            "AND t.status != :doneStatus " +
            "AND t.dueDate <= :endOfToday " +
            "ORDER BY t.dueDate ASC")
    List<TaskAssignee> findUrgentTasksByUserId(
            @Param("userId") Long userId,
            @Param("spaceId") Long spaceId,
            @Param("doneStatus") TaskStatus doneStatus,
            @Param("endOfToday") LocalDateTime endOfToday,
            Pageable pageable);

    // 진행중인 내 작업 목록 조회
    @Query("SELECT ta FROM TaskAssignee ta " +
            "JOIN FETCH ta.task t " +
            "JOIN FETCH t.project p " +
            "WHERE ta.user.id = :userId " +
            "AND p.space.id = :spaceId " +
            "AND t.status = :inProgressStatus " +
            "ORDER BY t.updatedAt DESC")
    List<TaskAssignee> findInProgressTasksByUserId(
            @Param("userId") Long userId,
            @Param("spaceId") Long spaceId,
            @Param("inProgressStatus") TaskStatus inProgressStatus,
            Pageable pageable);

    // 조건별 내 작업 개수 카운트
    @Query("SELECT COUNT(ta) FROM TaskAssignee ta " +
            "JOIN ta.task t " +
            "JOIN t.project p " +
            "WHERE ta.user.id = :userId " +
            "AND p.space.id = :spaceId")
    long countByUserIdAndSpaceId(
            @Param("userId") Long userId,
            @Param("spaceId") Long spaceId);

    @Query("SELECT COUNT(ta) FROM TaskAssignee ta " +
            "JOIN ta.task t " +
            "JOIN t.project p " +
            "WHERE ta.user.id = :userId " +
            "AND p.space.id = :spaceId " +
            "AND t.status = :status")
    long countByUserIdAndSpaceIdAndStatus(
            @Param("userId") Long userId,
            @Param("spaceId") Long spaceId,
            @Param("status") TaskStatus status);

    @Query("SELECT COUNT(ta) FROM TaskAssignee ta " +
            "JOIN ta.task t " +
            "JOIN t.project p " +
            "WHERE ta.user.id = :userId " +
            "AND p.space.id = :spaceId " +
            "AND t.status != :doneStatus " +
            "AND t.dueDate < :startOfToday")
    long countDelayedTasks(
            @Param("userId") Long userId,
            @Param("spaceId") Long spaceId,
            @Param("doneStatus") TaskStatus doneStatus,
            @Param("startOfToday") LocalDateTime startOfToday);

    // 내 작업 페이징 조회
    @Query(value = "SELECT ta FROM TaskAssignee ta " +
            "JOIN FETCH ta.task t " +
            "JOIN FETCH t.project p " +
            "WHERE ta.user.id = :userId " +
            "AND p.space.id = :spaceId " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:startDate IS NULL OR t.dueDate >= :startDate) " +
            "AND (:endDate IS NULL OR t.dueDate <= :endDate)", countQuery = "SELECT COUNT(ta) FROM TaskAssignee ta " +
                    "JOIN ta.task t " +
                    "JOIN t.project p " +
                    "WHERE ta.user.id = :userId " +
                    "AND p.space.id = :spaceId " +
                    "AND (:status IS NULL OR t.status = :status) " +
                    "AND (:startDate IS NULL OR t.dueDate >= :startDate) " +
                    "AND (:endDate IS NULL OR t.dueDate <= :endDate)")
    Page<TaskAssignee> findMyTasksByUserIdAndSpaceId(
            @Param("userId") Long userId,
            @Param("spaceId") Long spaceId,
            @Param("status") TaskStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
