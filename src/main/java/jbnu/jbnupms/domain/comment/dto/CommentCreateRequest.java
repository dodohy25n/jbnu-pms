package jbnu.jbnupms.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "댓글 생성 요청")
public class CommentCreateRequest {

    @NotNull(message = "Task ID는 필수입니다")
    @Schema(description = "Task ID", example = "1")
    private Long taskId;

    @Schema(description = "부모 댓글 ID (대댓글인 경우)", example = "1", nullable = true)
    private Long parentId;

    @NotBlank(message = "댓글 내용은 필수입니다")
    @Size(max = 1000, message = "댓글은 최대 1000자까지 입력 가능합니다")
    @Schema(description = "댓글 내용", example = "이 작업은 다음 주까지 완료 가능할 것 같습니다.")
    private String content;
}