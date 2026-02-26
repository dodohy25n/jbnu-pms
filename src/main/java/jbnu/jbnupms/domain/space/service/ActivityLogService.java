package jbnu.jbnupms.domain.space.service;

import jbnu.jbnupms.common.exception.CustomException;
import jbnu.jbnupms.common.exception.ErrorCode;
import jbnu.jbnupms.domain.space.dto.ActivitySummaryDto;
import jbnu.jbnupms.domain.space.entity.ActionType;
import jbnu.jbnupms.domain.space.entity.ActivityLog;
import jbnu.jbnupms.domain.space.entity.Space;
import jbnu.jbnupms.domain.space.repository.ActivityLogRepository;
import jbnu.jbnupms.domain.space.repository.SpaceMemberRepository;
import jbnu.jbnupms.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final SpaceMemberRepository spaceMemberRepository;

    @Transactional
    public void logActivity(Space space, Long projectId, String projectTitle, Long taskId, String taskTitle,
            ActionType actionType, User actor, String summary) {
        ActivityLog log = ActivityLog.builder()
                .space(space)
                .projectId(projectId)
                .projectTitle(projectTitle)
                .taskId(taskId)
                .taskTitle(taskTitle)
                .actionType(actionType)
                .actor(actor)
                .summary(summary)
                .build();
        activityLogRepository.save(log);
    }

    // 스케줄러용: actor 없이 시스템 이벤트 기록 (당일 중복 방지)
    @Transactional
    public void logSystemActivity(Space space, Long projectId, String projectTitle, Long taskId, String taskTitle,
            ActionType actionType, String summary) {
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfToday = LocalDateTime.now().with(LocalTime.MAX);

        if (activityLogRepository.existsByTaskIdAndActionTypeAndCreatedAtBetween(
                taskId, actionType, startOfToday, endOfToday)) {
            return;
        }

        ActivityLog log = ActivityLog.builder()
                .space(space)
                .projectId(projectId)
                .projectTitle(projectTitle)
                .taskId(taskId)
                .taskTitle(taskTitle)
                .actionType(actionType)
                .actor(null)
                .summary(summary)
                .build();
        activityLogRepository.save(log);
    }

    public List<ActivitySummaryDto> getRecentActivities(Long userId, Long spaceId) {
        validateSpaceMember(userId, spaceId);

        Pageable pageable = PageRequest.of(0, 5);
        return activityLogRepository.findBySpaceIdOrderByCreatedAtDesc(spaceId, pageable)
                .stream()
                .map(ActivitySummaryDto::from)
                .collect(Collectors.toList());
    }

    public Page<ActivitySummaryDto> getActivities(Long userId, Long spaceId, Pageable pageable) {
        validateSpaceMember(userId, spaceId);
        return activityLogRepository.findBySpaceIdOrderByCreatedAtDesc(spaceId, pageable).map(ActivitySummaryDto::from);
    }

    private void validateSpaceMember(Long userId, Long spaceId) {
        if (!spaceMemberRepository.existsBySpaceIdAndUserId(spaceId, userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 스페이스의 멤버가 아닙니다.");
        }
    }
}
