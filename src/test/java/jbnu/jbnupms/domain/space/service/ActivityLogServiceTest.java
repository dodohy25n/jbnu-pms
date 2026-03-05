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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @InjectMocks private ActivityLogService activityLogService;
    @Mock private ActivityLogRepository activityLogRepository;
    @Mock private SpaceMemberRepository spaceMemberRepository;

    private User buildUser(String name) {
        return User.builder().email(name + "@test.com").password("pw").name(name).provider("EMAIL").build();
    }
    private Space buildSpace(User owner) {
        return Space.builder().name("스페이스").description("설명").owner(owner).build();
    }

    @Test
    @DisplayName("활동 로그 기록 성공")
    void logActivity_success() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);

        activityLogService.logActivity(space, 1L, "프로젝트", 1L, "태스크",
                ActionType.TASK_COMPLETED, user, "작업 완료");

        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    @DisplayName("시스템 활동 로그 - 당일 중복이면 저장 안 함")
    void logSystemActivity_skip_ifDuplicate() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);

        // ActionType에 실제로 존재하는 값 사용: TASK_DUE_REACHED
        given(activityLogRepository.existsByTaskIdAndActionTypeAndCreatedAtBetween(
                eq(1L), eq(ActionType.TASK_DUE_REACHED), any(), any()))
                .willReturn(true);

        activityLogService.logSystemActivity(space, 1L, "프로젝트", 1L, "태스크",
                ActionType.TASK_DUE_REACHED, "마감 당일");

        verify(activityLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("시스템 활동 로그 - 중복 없으면 저장")
    void logSystemActivity_save_ifNoDuplicate() {
        User user = buildUser("홍길동");
        Space space = buildSpace(user);

        given(activityLogRepository.existsByTaskIdAndActionTypeAndCreatedAtBetween(
                eq(1L), eq(ActionType.TASK_OVERDUE), any(), any()))
                .willReturn(false);

        activityLogService.logSystemActivity(space, 1L, "프로젝트", 1L, "태스크",
                ActionType.TASK_OVERDUE, "마감 지연");

        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    @DisplayName("최근 활동 조회 성공")
    void getRecentActivities_success() {
        given(spaceMemberRepository.existsBySpaceIdAndUserId(1L, 1L)).willReturn(true);
        given(activityLogRepository.findBySpaceIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        List<ActivitySummaryDto> result = activityLogService.getRecentActivities(1L, 1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("최근 활동 조회 실패 - 스페이스 멤버 아님")
    void getRecentActivities_fail_notSpaceMember() {
        given(spaceMemberRepository.existsBySpaceIdAndUserId(1L, 1L)).willReturn(false);

        assertThatThrownBy(() -> activityLogService.getRecentActivities(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("활동 목록 페이징 조회 성공")
    void getActivities_success() {
        Pageable pageable = PageRequest.of(0, 10);

        given(spaceMemberRepository.existsBySpaceIdAndUserId(1L, 1L)).willReturn(true);
        given(activityLogRepository.findBySpaceIdOrderByCreatedAtDesc(eq(1L), eq(pageable)))
                .willReturn(new PageImpl<>(List.of()));

        Page<ActivitySummaryDto> result = activityLogService.getActivities(1L, 1L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(0);
    }
}