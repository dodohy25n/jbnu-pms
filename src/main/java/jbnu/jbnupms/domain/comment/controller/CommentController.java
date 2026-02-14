package jbnu.jbnupms.domain.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jbnu.jbnupms.common.response.CommonResponse;
import jbnu.jbnupms.domain.comment.dto.CommentCreateRequest;
import jbnu.jbnupms.domain.comment.dto.CommentResponse;
import jbnu.jbnupms.domain.comment.dto.CommentUpdateRequest;
import jbnu.jbnupms.domain.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 생성")
    @PostMapping
    public ResponseEntity<CommonResponse<CommentResponse>> createComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CommentCreateRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        CommentResponse response = commentService.createComment(request, userId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(summary = "Task의 댓글 목록 조회")
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<CommonResponse<List<CommentResponse>>> getCommentsByTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        List<CommentResponse> responses = commentService.getCommentsByTask(taskId, userId);
        return ResponseEntity.ok(CommonResponse.success(responses));
    }

    @Operation(summary = "댓글 수정")
    @PutMapping("/{commentId}")
    public ResponseEntity<CommonResponse<CommentResponse>> updateComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        CommentResponse response = commentService.updateComment(commentId, request, userId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommonResponse<Void>> deleteComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}