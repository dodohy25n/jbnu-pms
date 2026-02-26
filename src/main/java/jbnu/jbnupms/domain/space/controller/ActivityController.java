package jbnu.jbnupms.domain.space.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jbnu.jbnupms.common.response.CommonResponse;
import jbnu.jbnupms.domain.space.dto.ActivitySummaryDto;
import jbnu.jbnupms.domain.space.service.ActivityLogService;
import org.springframework.security.core.userdetails.UserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Activity Log", description = "활동 로그 조회 API")
@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityLogService activityLogService;

    @Operation(summary = "홈 화면 최근 업데이트 조회", description = "해당 스페이스의 최신 활동 로그를 5개 반환합니다.")
    @GetMapping("/recent")
    public ResponseEntity<CommonResponse<List<ActivitySummaryDto>>> getRecentActivities(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("spaceId") Long spaceId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        List<ActivitySummaryDto> response = activityLogService.getRecentActivities(spaceId, userId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(summary = "전체 업데이트 더보기", description = "전체 활동 로그를 페이징하여 조회합니다.")
    @GetMapping
    public ResponseEntity<CommonResponse<Page<ActivitySummaryDto>>> getActivities(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("spaceId") Long spaceId,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Page<ActivitySummaryDto> response = activityLogService.getActivities(spaceId, userId, pageable);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
