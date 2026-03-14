package jbnu.jbnupms.domain.notification.service;

import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.notification.dto.NotificationResponse;
import jbnu.jbnupms.domain.notification.entity.Notification;
import jbnu.jbnupms.domain.notification.entity.NotificationType;
import jbnu.jbnupms.domain.notification.repository.NotificationRepository;
import jbnu.jbnupms.domain.user.entity.User;
import jbnu.jbnupms.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository         userRepository;

    // 이 Pod에 SSE 연결된 유저 관리
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // SSE 구독
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30분

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout   (() -> emitters.remove(userId));
        emitter.onError     (e  -> emitters.remove(userId));

        emitters.put(userId, emitter);

        // 연결 직후 읽지 않은 개수 전달 (프론트 뱃지 초기화용)
        try {
            long unreadCount = notificationRepository.countByReceiverIdAndIsReadFalse(userId);
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("unreadCount", unreadCount)));
        } catch (Exception e) {
            emitters.remove(userId);
        }

        return emitter;
    }

    // 알림 저장 + SSE 전달
    @Transactional
    public void send(Long receiverId, NotificationType type,
                     String title, String content, String relatedUrl) {
        User receiver = userRepository.findById(receiverId).orElse(null);
        if (receiver == null) {
            log.warn("알림 수신자 없음. receiverId={}", receiverId);
            return;
        }

        // 1. DB 저장
        Notification notification = Notification.builder()
                .receiver  (receiver)
                .type      (type)
                .title     (title)
                .content   (content)
                .relatedUrl(relatedUrl)
                .build();
        notificationRepository.save(notification);

        // 2. 연결된 경우 SSE로 바로 전달
        pushToEmitter(receiverId, NotificationResponse.from(notification));
    }

    private void pushToEmitter(Long userId, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event().name("notification").data(data));
        } catch (Exception e) {
            emitters.remove(userId);
            log.warn("SSE 전송 실패. userId={}", userId);
        }
    }

    // 조회
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository
                .findByReceiverIdOrderByIsReadAscCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    // 읽음 처리
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getReceiver().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByReceiverId(userId);
    }
}