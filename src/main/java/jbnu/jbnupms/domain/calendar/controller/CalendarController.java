package jbnu.jbnupms.domain.calendar.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jbnu.jbnupms.common.response.CommonResponse;
import jbnu.jbnupms.domain.calendar.dto.CalendarItemDto;
import jbnu.jbnupms.domain.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Calendar", description = "캘린더 API")
@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @Operation(summary = "캘린더 조회", description = "지정한 월의 프로젝트/태스크 마감일을 조회합니다. spaceId를 지정하면 해당 스페이스만, 생략하면 내가 속한 전체 스페이스를 대상으로 합니다.")
    @GetMapping
    public ResponseEntity<CommonResponse<List<CalendarItemDto>>> getCalendarItems(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long spaceId,
            @RequestParam int year,
            @RequestParam int month) {
        Long userId = Long.parseLong(userDetails.getUsername());
        List<CalendarItemDto> response = calendarService.getCalendarItems(userId, spaceId, year, month);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
