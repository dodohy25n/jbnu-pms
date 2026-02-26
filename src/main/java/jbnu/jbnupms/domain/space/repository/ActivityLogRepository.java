package jbnu.jbnupms.domain.space.repository;

import jbnu.jbnupms.domain.space.entity.ActionType;
import jbnu.jbnupms.domain.space.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    Page<ActivityLog> findBySpaceIdOrderByCreatedAtDesc(Long spaceId, Pageable pageable);

    // 특정 태스크에 대해 당일 이미 동일 이벤트가 기록됐는지 확인 (중복 방지용)
    boolean existsByTaskIdAndActionTypeAndCreatedAtBetween(
            Long taskId, ActionType actionType,
            LocalDateTime startDate, LocalDateTime endDate);
}
