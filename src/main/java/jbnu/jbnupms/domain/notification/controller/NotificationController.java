package jbnu.jbnupms.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jbnu.jbnupms.common.response.CommonResponse;
import jbnu.jbnupms.domain.notification.dto.NotificationResponse;
import jbnu.jbnupms.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "SSE 알림 구독",
            description = "프론트엔드에서 앱 실행 시 한 번 호출하면 이후 알림을 실시간으로 수신합니다. 연결이 끊기면 재호출이 필요합니다."
    )
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return notificationService.subscribe(userId);
    }

    @Operation(summary = "알림 목록 조회")
    @GetMapping
    public ResponseEntity<CommonResponse<Page<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(CommonResponse.success(
                notificationService.getNotifications(userId, pageable)));
    }

    @Operation(summary = "읽지 않은 알림 개수")
    @GetMapping("/unread-count")
    public ResponseEntity<CommonResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(CommonResponse.success(
                notificationService.getUnreadCount(userId)));
    }

    @Operation(summary = "단건 읽음 처리")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<CommonResponse<Void>> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long notificationId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    @Operation(summary = "전체 읽음 처리")
    @PatchMapping("/read-all")
    public ResponseEntity<CommonResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}