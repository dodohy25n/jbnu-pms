package jbnu.jbnupms.domain.task.controller;

import jakarta.validation.Valid;
import jbnu.jbnupms.common.response.CommonResponse;
import jbnu.jbnupms.domain.task.dto.TaskCreateRequest;
import jbnu.jbnupms.domain.task.dto.TaskResponse;
import jbnu.jbnupms.domain.task.dto.TaskUpdateRequest;
import jbnu.jbnupms.domain.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    // 태스크 생성
    @PostMapping
    public ResponseEntity<CommonResponse<Long>> createTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TaskCreateRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Long taskId = taskService.createTask(userId, request);
        return ResponseEntity.ok(CommonResponse.success(taskId));
    }

    // 프로젝트별 태스크 목록 조회
    @GetMapping
    public ResponseEntity<CommonResponse<List<TaskResponse>>> getTasks(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long projectId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(CommonResponse.success(taskService.getTasks(userId, projectId)));
    }

    // 태스크 단건 조회
    @GetMapping("/{taskId}")
    public ResponseEntity<CommonResponse<TaskResponse>> getTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(CommonResponse.success(taskService.getTask(userId, taskId)));
    }

    // 태스크 수정
    @PutMapping("/{taskId}")
    public ResponseEntity<CommonResponse<Void>> updateTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId,
            @RequestBody TaskUpdateRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        taskService.updateTask(userId, taskId, request);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // 태스크 삭제
    @DeleteMapping("/{taskId}")
    public ResponseEntity<CommonResponse<Void>> deleteTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        taskService.deleteTask(userId, taskId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // 담당자 추가
    @PostMapping("/{taskId}/assignees")
    public ResponseEntity<CommonResponse<Void>> addAssignee(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId,
            @RequestParam Long assigneeId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        taskService.addAssignee(userId, taskId, assigneeId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // 담당자 삭제
    @DeleteMapping("/{taskId}/assignees/{assigneeId}")
    public ResponseEntity<CommonResponse<Void>> removeAssignee(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId,
            @PathVariable Long assigneeId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        taskService.removeAssignee(userId, taskId, assigneeId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}
