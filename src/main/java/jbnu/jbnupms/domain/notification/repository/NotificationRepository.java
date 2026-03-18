package jbnu.jbnupms.domain.notification.repository;

import jbnu.jbnupms.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.receiver.id = :receiverId " +
            "ORDER BY n.isRead ASC, n.createdAt DESC")
    Page<Notification> findByReceiverIdOrderByIsReadAscCreatedAtDesc(
            @Param("receiverId") Long receiverId, Pageable pageable);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.receiver.id = :receiverId AND n.isRead = false")
    void markAllAsReadByReceiverId(@Param("receiverId") Long receiverId);
}