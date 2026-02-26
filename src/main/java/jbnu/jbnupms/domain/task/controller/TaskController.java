package jbnu.jbnupms.domain.task.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jbnu.jbnupms.common.response.CommonResponse;
import jbnu.jbnupms.domain.task.dto.TaskCreateRequest;
import jbnu.jbnupms.domain.task.dto.TaskResponse;
import jbnu.jbnupms.domain.task.dto.TaskSummaryDto;
import jbnu.jbnupms.domain.task.dto.TaskUpdateRequest;
import jbnu.jbnupms.domain.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import jbnu.jbnupms.domain.task.dto.MyTaskSummaryDto;
import jbnu.jbnupms.domain.task.entity.TaskStatus;

import java.util.List;

@Tag(name = "Task", description = "태스크 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "태스크 생성")
    @PostMapping
    public ResponseEntity<CommonResponse<Long>> createTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TaskCreateRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Long taskId = taskService.createTask(userId, request);
        return ResponseEntity.ok(CommonResponse.success(taskId));
    }

    @Operation(summary = "프로젝트별 태스크 목록 조회")
    @GetMapping
    public ResponseEntity<CommonResponse<List<TaskResponse>>> getTasks(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long projectId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(CommonResponse.success(taskService.getTasks(userId, projectId)));
    }

    @Operation(summary = "태스크 단건 조회")
    @GetMapping("/{taskId}")
    public ResponseEntity<CommonResponse<TaskResponse>> getTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(CommonResponse.success(taskService.getTask(userId, taskId)));
    }

    @Operation(summary = "태스크 수정")
    @PutMapping("/{taskId}")
    public ResponseEntity<CommonResponse<Void>> updateTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId,
            @RequestBody TaskUpdateRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        taskService.updateTask(userId, taskId, request);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    @Operation(summary = "태스크 삭제")
    @DeleteMapping("/{taskId}")
    public ResponseEntity<CommonResponse<Void>> deleteTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        taskService.deleteTask(userId, taskId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    @Operation(summary = "담당자 추가")
    @PostMapping("/{taskId}/assignees")
    public ResponseEntity<CommonResponse<Void>> addAssignee(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId,
            @RequestParam Long assigneeId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        taskService.addAssignee(userId, taskId, assigneeId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    @Operation(summary = "담당자 삭제")
    @DeleteMapping("/{taskId}/assignees/{assigneeId}")
    public ResponseEntity<CommonResponse<Void>> removeAssignee(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId,
            @PathVariable Long assigneeId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        taskService.removeAssignee(userId, taskId, assigneeId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    @Operation(summary = "긴급 작업 목록 조회", description = "기한이 촉박하거나 지연된 긴급 작업 목록을 조회합니다. (최대 5개)")
    @GetMapping("/urgent")
    public ResponseEntity<CommonResponse<List<TaskSummaryDto>>> getUrgentTasks(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long spaceId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        List<TaskSummaryDto> response = taskService.getUrgentTasks(userId, spaceId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(summary = "내 작업 목록 조회", description = "스페이스 기준 담당자가 나인 작업을 범위별(TODAY, WEEK, ALL) 및 상태별로 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<CommonResponse<Page<TaskSummaryDto>>> getMyTasks(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long spaceId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false, defaultValue = "ALL") String range,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Page<TaskSummaryDto> response = taskService.getMyTasks(userId, spaceId, status, range, pageable);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(summary = "내 작업 요약", description = "스페이스 기준 내 작업의 상태별 개수 요약을 조회합니다.")
    @GetMapping("/my/summary")
    public ResponseEntity<CommonResponse<MyTaskSummaryDto>> getMyTaskSummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long spaceId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        MyTaskSummaryDto response = taskService.getMyTaskSummary(userId, spaceId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

}
