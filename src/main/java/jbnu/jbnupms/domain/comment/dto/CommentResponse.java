package jbnu.jbnupms.domain.comment.dto;

import jbnu.jbnupms.domain.comment.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@Schema(description = "댓글 응답")
public class CommentResponse {

    @Schema(description = "댓글 ID", example = "1")
    private Long id;

    @Schema(description = "Task ID", example = "1")
    private Long taskId;

    @Schema(description = "작성자 ID", example = "1")
    private Long userId;

    @Schema(description = "작성자 이름", example = "홍길동")
    private String userName;

    @Schema(description = "작성자 이메일", example = "user@example.com")
    private String userEmail;

    @Schema(description = "부모 댓글 ID (대댓글인 경우)", example = "1", nullable = true)
    private Long parentId;

    @Schema(description = "댓글 내용", example = "이 작업은 다음 주까지 완료 가능할 것 같습니다.")
    private String content;

    @Schema(description = "생성일시", example = "2024-02-13T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-02-13T10:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "대댓글 목록")
    private List<CommentResponse> replies;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .taskId(comment.getTask().getId())
                .userId(comment.getUser().getId())
                .userName(comment.getUser().getName())
                .userEmail(comment.getUser().getEmail())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(new ArrayList<>())
                .build();
    }

    public void addReply(CommentResponse reply) {
        this.replies.add(reply);
    }
}