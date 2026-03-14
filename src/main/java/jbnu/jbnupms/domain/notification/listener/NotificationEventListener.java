package jbnu.jbnupms.domain.notification.listener;

import jbnu.jbnupms.domain.notification.entity.NotificationType;
import jbnu.jbnupms.domain.notification.event.*;
import jbnu.jbnupms.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentCreatedEvent event) {
        String preview = event.getCommentContent().length() > 50
                ? event.getCommentContent().substring(0, 50) + "..."
                : event.getCommentContent();

        event.getReceiverIds().forEach(receiverId ->
                notificationService.send(
                        receiverId,
                        NotificationType.COMMENT,
                        "[" + event.getTaskTitle() + "] 새 댓글",
                        event.getCommentAuthorName() + ": " + preview,
                        "/tasks/" + event.getTaskId()
                )
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskAssigned(TaskAssignedEvent event) {
        notificationService.send(
                event.getAssigneeId(),
                NotificationType.TASK_ASSIGN,
                "태스크 담당자로 배정되었습니다",
                event.getAssignerName() + "님이 [" + event.getTaskTitle() + "] 태스크를 배정했습니다.",
                "/projects/" + event.getProjectId() + "/tasks/" + event.getTaskId()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSpaceInvited(SpaceInvitedEvent event) {
        notificationService.send(
                event.getInvitedUserId(),
                NotificationType.SPACE_INVITE,
                "스페이스에 초대되었습니다",
                "[" + event.getSpaceName() + "] 스페이스에 초대되었습니다.",
                "/spaces/" + event.getSpaceId()
        );
    }

    // fallbackExecution = true : 스케줄러처럼 트랜잭션 없는 곳에서도 동작
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTaskDue(TaskDueEvent event) {
        NotificationType type  = event.isOverdue() ? NotificationType.TASK_OVERDUE  : NotificationType.TASK_DUE;
        String           title = event.isOverdue() ? "태스크 마감일이 초과되었습니다"  : "태스크 마감일이 도래했습니다";

        event.getAssigneeIds().forEach(receiverId ->
                notificationService.send(
                        receiverId, type, title,
                        "[" + event.getTaskTitle() + "]",
                        "/tasks/" + event.getTaskId()
                )
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onProjectDue(ProjectDueEvent event) {
        NotificationType type  = event.isOverdue() ? NotificationType.PROJECT_OVERDUE  : NotificationType.PROJECT_DUE;
        String           title = event.isOverdue() ? "프로젝트 마감일이 초과되었습니다" : "프로젝트 마감일이 도래했습니다";

        event.getMemberIds().forEach(receiverId ->
                notificationService.send(
                        receiverId, type, title,
                        "[" + event.getProjectName() + "]",
                        "/projects/" + event.getProjectId()
                )
        );
    }
}