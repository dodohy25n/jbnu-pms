package jbnu.jbnupms.domain.space.repository;

import jbnu.jbnupms.domain.space.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    Page<ActivityLog> findBySpaceIdOrderByCreatedAtDesc(Long spaceId, Pageable pageable);
}
